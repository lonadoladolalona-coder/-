# Nursing Roster – October to December 2026

- `Nursing_Roster_Oct-Dec_2026.xlsx`: editable roster with one sheet per month (OCT, NOV, DEC).
- `Nursing_Roster_Oct-Dec_2026.pdf`: print-ready copy on A4 landscape, one page per month.
- `ICU1ST_Roster_Oct-Dec_2026.xlsx` / `.pdf`: ICU 1st staff only (Sourabh to Abigel), same duties.
- `generate_roster.py`: rebuilds both workbooks from the September 2026 roster.

## How it continues September

The script follows the rules used on the September sheet:

- Each nurse keeps the same weekly off day (O).
- After each off day the shift rotates **A → C → B → A**.
- Abigel stays on General (G) with Sundays off. Swati stays on A with Wednesdays off.
- Sangeeta G. resigned in September, so she is not rostered.

Under these rules the script reproduces every active nurse's September row exactly.
The one exception is Swati, who worked Wed 30 Sep, her usual off day.

Below each month there is a **STAFF ON DUTY** table that uses COUNTIF to count
A/B/C/G per unit per day. If you swap duties in Excel, the counts update automatically.

To regenerate: `python3 generate_roster.py`. To make the PDF, open the file in
Excel or LibreOffice and export it.
