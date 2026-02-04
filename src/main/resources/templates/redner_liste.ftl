<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>${titel}</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
    <nav class="navbar">
        <div class="nav-links">
            <a href="/">Startseite</a>
            <a href="/abgeordnete" style="color: var(--primary)">Alle Abgeordnete</a>
            <a href="/stats">Statistik</a>
        </div>
    </nav>

    <div class="container">
        <div class="section-header">
            <h2>Alle Abgeordnete</h2>
            <span class="badge">${rednerListe?size} Einträge</span>
        </div>

        <form action="/abgeordnete" method="get" class="search-wrapper" style="margin-bottom: 2rem; max-width: 100%; box-shadow: var(--shadow-soft);">
            <input type="text" name="q" class="search-input"
                   placeholder="Namen suchen..."
                   value="${currentSearch!}"
                   autocomplete="off">

            <div class="select-wrapper">
                <select name="sort" class="search-select" onchange="this.form.submit()">
                    <option value="name" <#if (currentSort!'name') == 'name'>selected</#if>>Name (A-Z)</option>
                    <option value="name_desc" <#if (currentSort!'') == 'name_desc'>selected</#if>>Name (Z-A)</option>
                </select>
            </div>

            <button type="submit" class="search-btn">Suchen</button>
        </form>

        <div class="grid">
            <#list rednerListe as a>
                <a href="/abgeordneter/${a.id}" class="card">
                    <div class="card-header">
                        <h3 class="card-title"> ${a.vollerName}</h3>
                        <#if a.toNode().fraktion?has_content>
                            <span class="card-subtitle">${a.toNode().fraktion}</span>
                        <#else>
                            <span class="card-subtitle">Fraktionslos</span>
                        </#if>
                    </div>
                </a>
            <#else>
                <div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 3rem;">
                    <p>Keine Abgeordneten gefunden.</p>
                    <a href="/abgeordnete" style="color: var(--primary); text-decoration: none;">Filter zurücksetzen</a>
                </div>
            </#list>
        </div>
    </div>
</body>
</html>