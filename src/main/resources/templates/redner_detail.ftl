<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>${redner.vollerName}</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="/" class="brand">RedenPortal</a>
        <div class="nav-links">
            <a href="/">Startseite</a>
            <a href="/abgeordnete">Alle Abgeordnete</a>
            <a href="/stats">Statistik</a>
        </div>
    </nav>

    <div class="container" style="margin-top: 3rem;">
        <div class="speech-content" style="padding: 2rem; text-align: center; margin-bottom: 3rem;">
            <h1 style="margin:0; font-size: 2.5rem;">${redner.vollerName}</h1>

            <p style="color: var(--text-muted); margin-top: 0.5rem; font-size: 1.2rem;">
                <#if redner.fraktion?has_content>
                    Fraktion: <strong style="color:var(--primary);">${redner.fraktion}</strong>
                <#else>
                    Funktion: <strong>Regierungsmitglied / Gast</strong>
                </#if>
            </p>
            <div class="hero-stats" style="margin-top: 2rem;
margin-bottom:0;">
                <div class="hero-stat-item" style="background:var(--primary);
color:white;">${reden?size} Reden gehalten</div>
            </div>
        </div>

        <div class="section-header">
            <h2>Gehaltene Reden</h2>
        </div>

        <div class="grid">
            <#list reden as r>
                <a href="/rede/${r.id}" class="card">
                    <h3 style="margin-top:0;
color: var(--primary);">Rede ID: ${r.id}</h3>
                    <p style="color: var(--text-muted);
font-size:0.9rem;">
                        <#--  Sicherer Zugriff auf verschachtelte Sitzungsdaten -->
                        <#if r.sitzung??>
                            Sitzung: ${r.sitzung.wahlperiode!}/${r.sitzung.sitzungNr!}, ${r.sitzung.datum!}
                        <#else>
                            Hier klicken für den Volltext.
                        </#if>
                    </p>
                    <span class="badge" style="margin-top: auto;">${r.kommentarAnzahl!0} Kommentare</span>
                </a>
            <#else>
                <p>Keine Reden in der Datenbank gefunden.</p>
            </#list>
        </div>
    </div>
</body>
</html>