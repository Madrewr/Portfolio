import asyncio
import pandas as pd
import re
from playwright.async_api import async_playwright

URL = "https://www.ceresne.sk/ponuka-byvania/"
OUTPUT_CSV = "ceresne_byty.csv"

def clean_header(text):
    """Remove arrows, linebreaks, and excess whitespace from header cell."""
    return re.sub(r'[\n▲▼]+', '', text).strip()

async def get_clean_headers(page):
    """Extracts and cleans all table headers from the page."""
    header_cells = await page.query_selector_all("table thead th")
    return [clean_header(await h.inner_text()) for h in header_cells]

async def get_rows(page):
    """Extracts all data rows from the page's table."""
    rows = await page.query_selector_all("table tbody tr")
    data = []
    for row in rows:
        cells = await row.query_selector_all("td")
        cell_data = [await c.inner_text() for c in cells]
        data.append([d.strip() for d in cell_data])
    return data

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        await page.goto(URL)

        # Accept cookies if the banner is present
        try:
            await page.click('button:has-text("Prijať všetky")', timeout=3000)
        except:
            pass

        # Detect number of pages
        await page.wait_for_selector("li.pagination-next")
        page_btns = await page.query_selector_all(
            'ul > li:not(.pagination-next):not(.pagination-previous)'
        )
        page_numbers = [
            int((await li.inner_text()).strip())
            for li in page_btns
            if (await li.inner_text()).strip().isdigit()
        ]
        total_pages = max(page_numbers)

        headers = await get_clean_headers(page)
        all_data = []

        # Loop through all pages and collect data
        for page_num in range(1, total_pages + 1):
            await page.wait_for_selector("table tbody tr", timeout=8000)
            all_data.extend(await get_rows(page))

            # Go to next page if not at the last
            if page_num < total_pages:
                btn_li = await page.query_selector(
                    f'ul > li:has-text("{page_num + 1}")'
                )
                if not btn_li:
                    break
                btn = await btn_li.query_selector("button")
                if not btn:
                    break
                await btn.click()
                await page.wait_for_timeout(300)

        await browser.close()

    df = pd.DataFrame(all_data, columns=headers)
    df.to_csv(OUTPUT_CSV, index=False, encoding="utf-8")
    print(f"Saved {len(all_data)} rows to {OUTPUT_CSV}")

if __name__ == "__main__":
    asyncio.run(main())
