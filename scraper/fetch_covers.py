#!/usr/bin/env python3
"""由披露易 (HKEXnews) 抓取年報 / 中期報告清單，下載 PDF，只保留第 1 頁做封面 JPG。

輸出（--out 目錄，即 data 分支）：
  years.json            [{"year": 2026, "count": 1234}, ...]
  index/<year>.json     [{id, code, name, title, type, date, pdf, cover}, ...]
  covers/<year>/<id>.jpg
  fails.json            {id: 失敗次數}
"""
import argparse
import hashlib
import html
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime, timedelta

try:
    import pymupdf as fitz
except ImportError:  # 舊版 PyMuPDF
    import fitz
import requests

BASE = "https://www1.hkexnews.hk"
SEARCH = BASE + "/search/titleSearchServlet.do"
TYPES = {"AR": "40100", "IR": "40200"}  # 年報 / 中期報告
ROWS = 1000
COVER_WIDTH = 600
MAX_PDF_BYTES = 300 * 1024 * 1024
MAX_FAILS = 3
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
    "Referer": BASE + "/search/titlesearch.xhtml?lang=zh",
    "Accept-Language": "zh-HK,zh;q=0.9,en;q=0.8",
}


def new_session():
    s = requests.Session()
    s.headers.update(HEADERS)
    return s


def search(session, d1, d2, t2code, rows):
    params = {
        "sortDir": "0", "sortByOptions": "DateTime", "category": "0",
        "market": "SEHK", "stockId": "-1", "documentType": "-1",
        "fromDate": d1.strftime("%Y%m%d"), "toDate": d2.strftime("%Y%m%d"),
        "title": "", "searchType": "1", "t1code": "40000", "t2Gcode": "-2",
        "t2code": t2code, "rowRange": str(rows), "lang": "zh",
    }
    last = None
    for attempt in range(4):
        try:
            r = session.get(SEARCH, params=params, timeout=60)
            r.raise_for_status()
            try:
                data = r.json()
            except ValueError:
                raise RuntimeError("回應唔係 JSON：" + r.text[:300])
            break
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(3 * (attempt + 1))
    else:
        raise RuntimeError(f"查詢失敗 {d1}~{d2}: {last}")
    result = data if isinstance(data, list) else data.get("result")
    if isinstance(result, str):
        result = json.loads(result) if result.strip() not in ("", "null") else []
    result = result or []
    total = len(result)
    if result:
        try:
            total = int(str(result[0].get("TOTAL_COUNT", total)).replace(",", ""))
        except ValueError:
            pass
    return result, total


def collect(session, d1, d2, t2code):
    recs, total = search(session, d1, d2, t2code, ROWS)
    if total > len(recs):
        if d1 < d2:
            mid = d1 + (d2 - d1) // 2
            return collect(session, d1, mid, t2code) + collect(session, mid + timedelta(days=1), d2, t2code)
        recs, _ = search(session, d1, d2, t2code, total + 50)
    time.sleep(0.5)
    return recs


def clean(s):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", s or ""))).strip()


def parts(s):
    return [p for p in (clean(x) for x in re.split(r"<br\s*/?>", s or "", flags=re.I)) if p]


def normalize(rec, rtype):
    link = (rec.get("FILE_LINK") or "").strip()
    if not link:
        return None
    url = link if link.startswith("http") else BASE + link
    rid = re.sub(r"[^0-9A-Za-z_-]", "", str(rec.get("NEWS_ID") or ""))
    if not rid:
        rid = hashlib.sha1(url.encode()).hexdigest()[:12]
    codes, names = parts(rec.get("STOCK_CODE")), parts(rec.get("STOCK_NAME"))
    try:
        d = datetime.strptime(clean(rec.get("DATE_TIME"))[:10], "%d/%m/%Y").strftime("%Y-%m-%d")
    except ValueError:
        d = ""
    return {
        "id": rid,
        "code": codes[0] if codes else "",
        "name": names[0] if names else "",
        "title": clean(rec.get("TITLE")),
        "type": rtype,
        "date": d,
        "pdf": url,
        "cover": "",
    }


def render_cover(pdf_path, out_path):
    doc = fitz.open(pdf_path)
    try:
        if doc.needs_pass or doc.page_count == 0:
            raise RuntimeError("PDF 加密或者無頁")
        page = doc[0]
        zoom = COVER_WIDTH / max(page.rect.width, 1)
        pix = page.get_pixmap(matrix=fitz.Matrix(zoom, zoom), alpha=False)
        os.makedirs(os.path.dirname(out_path), exist_ok=True)
        pix.save(out_path, output="jpeg", jpg_quality=80)
    finally:
        doc.close()


def fetch_cover(entry, out_path):
    """回傳 None = 成功；否則回傳錯誤字串。"""
    tmp = None
    try:
        s = new_session()
        with s.get(entry["pdf"], stream=True, timeout=120) as r:
            r.raise_for_status()
            fd, tmp = tempfile.mkstemp(suffix=".pdf")
            size = 0
            with os.fdopen(fd, "wb") as f:
                for chunk in r.iter_content(1 << 16):
                    size += len(chunk)
                    if size > MAX_PDF_BYTES:
                        raise RuntimeError("PDF 太大")
                    f.write(chunk)
        render_cover(tmp, out_path)
        time.sleep(0.3)
        return None
    except Exception as e:  # noqa: BLE001
        return f"{type(e).__name__}: {e}"[:200]
    finally:
        if tmp and os.path.exists(tmp):
            os.remove(tmp)


def load_json(path, default):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return default


def save_json(path, obj):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, separators=(",", ":"))


def save_all(out, year, entries, fails):
    rows = sorted(entries.values(), key=lambda e: (e["code"], e["date"], e["id"]))
    save_json(os.path.join(out, "index", f"{year}.json"), rows)
    save_json(os.path.join(out, "fails.json"), fails)
    years = []
    idx_dir = os.path.join(out, "index")
    for fn in sorted(os.listdir(idx_dir), reverse=True):
        if re.fullmatch(r"\d{4}\.json", fn):
            lst = load_json(os.path.join(idx_dir, fn), [])
            years.append({"year": int(fn[:4]), "count": sum(1 for e in lst if e.get("cover"))})
    save_json(os.path.join(out, "years.json"), years)


def git_sync(out, msg):
    def run(*a):
        return subprocess.run(["git", "-C", out, *a], capture_output=True, text=True)
    run("add", "-A")
    if run("diff", "--cached", "--quiet").returncode == 0:
        return
    run("commit", "-q", "-m", msg)
    p = run("push", "-q", "origin", "HEAD:data")
    if p.returncode != 0:
        print("⚠️ git push 失敗：", p.stderr.strip()[:300], flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--year", default="")
    ap.add_argument("--out", default="data")
    ap.add_argument("--git", action="store_true")
    ap.add_argument("--max-minutes", type=float, default=300)
    ap.add_argument("--workers", type=int, default=3)
    a = ap.parse_args()

    today = date.today()
    year = int(a.year) if str(a.year).strip() else today.year
    deadline = time.time() + a.max_minutes * 60
    out = a.out
    os.makedirs(os.path.join(out, "index"), exist_ok=True)

    idx_path = os.path.join(out, "index", f"{year}.json")
    entries = {e["id"]: e for e in load_json(idx_path, [])}
    fails = load_json(os.path.join(out, "fails.json"), {})

    # 1) 清單
    session = new_session()
    found = 0
    for rtype, t2 in TYPES.items():
        for m in range(1, 13):
            d1 = date(year, m, 1)
            if d1 > today:
                break
            d2 = (date(year + (m == 12), m % 12 + 1, 1) - timedelta(days=1))
            d2 = min(d2, today)
            recs = collect(session, d1, d2, t2)
            print(f"{rtype} {d1:%Y-%m}: {len(recs)} 份", flush=True)
            for rec in recs:
                e = normalize(rec, rtype)
                if not e:
                    continue
                found += 1
                old = entries.get(e["id"])
                if old:
                    e["cover"] = old.get("cover", "")
                entries[e["id"]] = e
    print(f"{year} 年清單共 {len(entries)} 份（今次搜到 {found}）", flush=True)
    if found == 0 and year < today.year:
        print("❌ 成年一份都搜唔到，披露易個接口可能改咗或者封咗呢個 IP。", flush=True)
        sys.exit(1)

    # 2) 封面
    for e in entries.values():
        if e["cover"] and not os.path.exists(os.path.join(out, e["cover"])):
            e["cover"] = ""
    todo = [e for e in entries.values()
            if not e["cover"] and e["pdf"].lower().split("?")[0].endswith(".pdf")
            and fails.get(e["id"], 0) < MAX_FAILS]
    print(f"要下載封面：{len(todo)} 份", flush=True)
    save_all(out, year, entries, fails)

    done = errs = 0
    last_sync = time.time()
    with ThreadPoolExecutor(max_workers=a.workers) as pool:
        for i in range(0, len(todo), 30):
            if time.time() > deadline:
                print("⏱ 到時間上限，下次再繼續。", flush=True)
                break
            batch = todo[i:i + 30]
            rels = [f"covers/{year}/{e['id']}.jpg" for e in batch]
            results = list(pool.map(lambda p: fetch_cover(p[0], os.path.join(out, p[1])), zip(batch, rels)))
            for e, rel, err in zip(batch, rels, results):
                if err is None:
                    e["cover"] = rel
                    fails.pop(e["id"], None)
                    done += 1
                else:
                    fails[e["id"]] = fails.get(e["id"], 0) + 1
                    errs += 1
                    print(f"  ✗ {e['code']} {e['title'][:30]}: {err}", flush=True)
            print(f"進度 {min(i + 30, len(todo))}/{len(todo)}（成功 {done}，失敗 {errs}）", flush=True)
            save_all(out, year, entries, fails)
            if a.git and time.time() - last_sync > 600:
                git_sync(out, f"covers {year}: +{done}")
                last_sync = time.time()

    save_all(out, year, entries, fails)
    if a.git:
        git_sync(out, f"covers {year}: +{done}")
    print(f"完成：新增 {done} 個封面，失敗 {errs}", flush=True)
    if todo and done == 0:
        sys.exit(1)


if __name__ == "__main__":
    main()
