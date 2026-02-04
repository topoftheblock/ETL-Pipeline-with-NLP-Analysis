<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${titel}</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="/" class="brand"><span></span> DigitalBundestag</a>
        <div class="nav-links">
            <a href="/">Suche zurücksetzen</a>
            <a href="/abgeordnete">Alle Abgeordnete</a>
            <a href="/stats">Statistik</a>
        </div>
    </nav>

    <div class="container">
        <header class="hero">
            <h1>Offenes Parlament.</h1>
            <p>Recherchieren Sie in Plenarprotokollen der aktuellen Wahlperiode.</p>

            <div class="hero-stats">
                <div class="hero-stat-item">👤 ${rednerCount!0} Abgeordnete</div>
                <div class="hero-stat-item">📄 ${count!0} Protokollierte Reden</div>
            </div>

            <form action="/" method="get" class="search-wrapper">
                <input type="text" name="search" class="search-input"
                       placeholder="Name des Abgeordneten..."
                       value="${search!}"
                       autocomplete="off">

                <div class="select-wrapper">
                    <select name="fraktion" class="search-select">
                        <option value="">Alle Fraktionen</option>
                        <#if fraktionenListe??>
                            <#list fraktionenListe as f>
                                <option value="${f}" <#if fraktion! == f>selected</#if>>${f}</option>
                            </#list>
                        </#if>
                    </select>
                </div>

                <button type="submit" class="search-btn">Finden</button>
            </form>
        </header>

        <div class="section-header">
            <h2>
                <#if search?has_content || fraktion?has_content>
                    Gefundene Profile
                <#else>
                    Abgeordnete
                </#if>
            </h2>

            <#if search?has_content || fraktion?has_content>
                <span class="section-hint">${abgeordnete?size} Treffer für Ihre Suche</span>
            </#if>
        </div>

        <div class="grid">
            <#list abgeordnete as a>
                <a href="/abgeordneter/${a.id}" class="card">
                    <div class="card-header">
                        <div style="display:flex; justify-content:space-between; align-items:start;">
                            <h3>${a.titel!} ${a.vorname!} <strong style="color:var(--primary);">${a.nachname!}</strong></h3>
                        </div>
                        <span class="card-subtitle">ID: ${a.id}</span>
                    </div>

                    <div style="flex-grow:1;"></div>

                    <#if a.fraktion?has_content>
                        <span class="badge primary">${a.fraktion}</span>
                    <#else>
                        <span class="badge">Fraktionslos / Gast</span>
                    </#if>
                </a>
            <#else>
                <div style="grid-column: 1/-1; text-align: center; padding: 4rem; color: var(--text-muted);">
                    <div style="font-size: 3rem; margin-bottom: 1rem;">🔍</div>
                    <h3>Keine Ergebnisse gefunden</h3>
                    <p>Versuchen Sie es mit einem anderen Namen oder setzen Sie den Filter zurück.</p>
                    <a href="/" style="display: inline-block; margin-top: 1rem; color: var(--primary); font-weight: 600; text-decoration: none;">Filter zurücksetzen &rarr;</a>
                </div>
            </#list>
        </div>
    </div>
</body>
</html>