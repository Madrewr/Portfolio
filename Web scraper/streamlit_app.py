
import streamlit as st
import pandas as pd
import numpy as np
import altair as alt

# Nastavenie celej aplikácie – široký layout, názov záložky
st.set_page_config(page_title="Analýza bytov", layout="wide")

# Načítanie a čistenie CSV súboru
@st.cache_data
def load_data():
    df = pd.read_csv("ceresne_byty.csv")

    # Prevedieme všetky stĺpce na veľké písmená a premenovávame na jednoduchšie názvy
    df.columns = [col.strip().upper() for col in df.columns]
    df = df.rename(columns={
        "FÁZA": "Faza", "TERMÍN": "Termin", "VR": "VR", "DOM": "Dom", "NÁZOV": "Nazov",
        "ORIENTÁCIA": "Orientacia", "PODLAŽIE": "Podlazie", "IZBY": "Izby",
        "INTERIÉR M²": "Interier_m2", "EXTERIÉR M²": "Exterier_m2", "SPOLU M²": "Spolu_m2",
        "ZVÝHODNENÁ CENA": "Zvyhodnena_cena", "CENNÍKOVÁ CENA": "Cennikova_cena",
        "STAV": "Stav", "POROVNAŤ": "Porovnat"
    })

    # Funkcia na očistenie čísiel od €, medzier, pomlčiek a prevod na float
    def clean_number(x):
        if pd.isna(x): return np.nan
        x_str = str(x).strip().replace("€", "").replace(" ", "").replace(",", ".")
        return float(x_str) if x_str not in ["", "-", "nan"] else np.nan

    # Aplikujeme čistenie na vybrané stĺpce
    for col in ["Interier_m2", "Exterier_m2", "Spolu_m2", "Zvyhodnena_cena", "Cennikova_cena"]:
        df[col] = df[col].apply(clean_number)

    # Počet izieb upravíme tiež, ak by boli s čiarkou
    df["Izby"] = df["Izby"].apply(lambda x: float(str(x).replace(",", ".").strip()) if pd.notna(x) else np.nan)
    return df

# Nahratie dát
df = load_data()

# Nadpis a sidebar filtre
st.title("🧱 Bytová Analýza pre Firmu")
st.sidebar.header("🔍 Filtrovanie")

# Filtrovanie podľa fázy, stavu, počtu izieb
faza = st.sidebar.multiselect("Fáza", sorted(df["Faza"].dropna().unique()), default=sorted(df["Faza"].dropna().unique()))
stav = st.sidebar.multiselect("Stav", sorted(df["Stav"].dropna().unique()), default=sorted(df["Stav"].dropna().unique()))
izby = st.sidebar.multiselect("Počet izieb", sorted(df["Izby"].dropna().unique()), default=sorted(df["Izby"].dropna().unique()))

# Aplikácia filtrov
df_filtered = df[df["Faza"].isin(faza) & df["Stav"].isin(stav) & df["Izby"].isin(izby)]

# Export tlačidlo a zobrazenie tabuľky
st.download_button("📥 Stiahni filtrované dáta ako CSV", df_filtered.to_csv(index=False).encode("utf-8"), file_name="byty_filtrovane.csv", mime="text/csv")
st.dataframe(df_filtered, use_container_width=True)

st.markdown("---")
st.header("📊 Firemná analytika")

# Pomocná funkcia – ak chýbajú dáta, zobrazí správu
def safe_chart(title, chart_func):
    st.subheader(title)
    try:
        chart_func()
    except Exception:
        st.info("🛈 Nedostatočné alebo chýbajúce dáta pre túto vizualizáciu.")

# Grafy iba ak sú dáta dostupné
if not df_filtered.empty:
    safe_chart("1️⃣ Počet bytov podľa stavu", lambda: st.bar_chart(df_filtered["Stav"].value_counts()))
    safe_chart("2️⃣ Priemerná zvýhodnená cena podľa fázy", lambda: st.bar_chart(df_filtered.groupby("Faza")["Zvyhodnena_cena"].mean()))
    safe_chart("3️⃣ Počet bytov podľa počtu izieb", lambda: st.bar_chart(df_filtered["Izby"].value_counts().sort_index()))
    safe_chart("4️⃣ Priemerná cena za m² podľa fázy", lambda: st.line_chart(
        df_filtered.groupby("Faza").apply(lambda x: x["Zvyhodnena_cena"].sum() / x["Spolu_m2"].sum())
    ))
    if "Orientacia" in df_filtered.columns:
        safe_chart("5️⃣ Priemerná výmera podľa orientácie", lambda: st.bar_chart(df_filtered.groupby("Orientacia")["Spolu_m2"].mean()))
    safe_chart("6️⃣ Počet bytov podľa podlažia", lambda: st.bar_chart(df_filtered["Podlazie"].value_counts().sort_index()))

    # Skutočný histogram cien – pomocou Altair a binovania
    safe_chart("7️⃣ Histogram zvýhodnených cien", lambda: st.altair_chart(
        alt.Chart(df_filtered.dropna(subset=["Zvyhodnena_cena"])).mark_bar().encode(
            alt.X("Zvyhodnena_cena:Q", bin=alt.Bin(maxbins=30), title="Cena (€)"),
            alt.Y("count():Q", title="Počet")
        ).properties(width=1280, height=400),
        use_container_width=False
    ))
else:
    st.info("Žiadne dáta pre zvolené filtre.")
