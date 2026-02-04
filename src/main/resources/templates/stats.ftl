<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>${titel}</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        /* Styles für das Statistik-Layout weil das nicht in style.css definiert ist. */
        .stats-container {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
            gap: 2rem;
            margin-top: 2rem;
        }
        .stats-card {
            background: white;
            border-radius: 16px;
            padding: 2rem;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
            border: 1px solid #e2e8f0;
        }
        .stats-card h3 {
            margin-top: 0;
            color: #2563eb;
        }

        /* Tabellen-stiles */
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }
        th, td {
            padding: 0.75rem;
            text-align: left;
            border-bottom: 1px solid #f1f5f9;
        }
        th {
            font-weight: 600;
            color: #64748b;
            font-size: 0.9rem;
            text-transform: uppercase;
        }
        td {
            font-size: 0.95rem;
        }
        tr:last-child td {
            border-bottom: none;
        }
        .num-col {
            text-align: right;
            font-family: monospace;
            font-weight: 600;
        }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="/" class="brand"> RedenPortal</a>
        <div class="nav-links">
            <a href="/">Suche</a>
            <a href="/abgeordnete">Abgeordnete</a>
            <a href="/stats" style="color: #2563eb;">Statistik</a>
        </div>
    </nav>

    <div class="container">
        <div class="section-header">
            <h2>Statistiken & Analysen</h2>
            <span class="section-hint">Automatisch generiert aus den Protokollen</span>
        </div>

        <div class="stat-highlight-box">
            <div>
                <span class="highlight-label">Längste Sitzung (Zeit)</span>
                <span class="highlight-val">
                    <#if maxTimeSession??>${maxTimeDuration} Min.<#else>N/A</#if>
                </span>
                <small><#if maxTimeSession??>${maxTimeSession.datum} (WP ${maxTimeSession.wahlperiode})</#if></small>
            </div>
            <div style="border-left: 1px solid rgba(255,255,255,0.3); height: 50px;"></div>
            <div>
                <span class="highlight-label">Längste Sitzung (Textmenge)</span>
                <span class="highlight-val">
                    <#if maxTextSession??>${maxTextLength}</#if>
                </span>
                <small><#if maxTextSession??>Zeichen am ${maxTextSession.datum}</#if></small>
            </div>
        </div>

        <div class="stats-container">
            <div class="stats-card">
                <h3> Redelänge Ø (Top 10 Redner)</h3>
                <table>
                    <thead><tr><th>Name</th><th>Fraktion</th><th class="num-col">Zeichen</th></tr></thead>
                    <tbody>
                        <#list topLength as item>
                            <tr>
                                <td><a href="/abgeordneter/${item.id}">${item.vollerName}</a></td>
                                <td>${item.fraktion!"-"}</td>
                                <td class="num-col">${item.value}</td>
                            </tr>
                        </#list>
                    </tbody>
                </table>
            </div>

            <div class="stats-card">
                <h3> Redelänge Ø (Partei)</h3>
                <table>
                    <thead><tr><th>Partei</th><th class="num-col">Zeichen</th></tr></thead>
                    <tbody>
                        <#list partyLength as item>
                            <tr>
                                <td><span class="badge">${item.name}</span></td>
                                <td class="num-col">${item.value}</td>
                            </tr>
                        </#list>
                    </tbody>
                </table>
            </div>

            <div class="stats-card">
                <h3>Meiste Kommentare Ø (Redner)</h3>
                <table>
                    <thead><tr><th>Name</th><th>Fraktion</th><th class="num-col">Anzahl</th></tr></thead>
                    <tbody>
                        <#list topComm as item>
                            <tr>
                                <td><a href="/abgeordneter/${item.id}">${item.vollerName}</a></td>
                                <td>${item.fraktion!"-"}</td>
                                <td class="num-col">${item.value}</td>
                            </tr>
                        </#list>
                    </tbody>
                </table>
            </div>

            <div class="stats-card">
                <h3>Meiste Kommentare Ø (Partei)</h3>
                <table>
                    <thead><tr><th>Partei</th><th class="num-col">Anzahl</th></tr></thead>
                    <tbody>
                        <#list partyComm as item>
                            <tr>
                                <td><span class="badge">${item.name}</span></td>
                                <td class="num-col">${item.value}</td>
                            </tr>
                        </#list>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
</html>