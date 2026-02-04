<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>${titel!"Rede Details"}</title>
    <link rel="stylesheet" href="/css/style.css">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <style>
        .charts-container { display: flex; flex-wrap: wrap; justify-content: space-around; margin-bottom: 2rem; padding: 1rem; background-color: #f9f9f9; border-radius: 8px; border: 1px solid #eee; }
        .chart-box { width: 30%; min-width: 300px; margin: 10px; text-align: center; }
        .chart-box h3 { margin-bottom: 10px; font-size: 1.1rem; color: #333; }
        .bar { fill: steelblue; transition: fill 0.2s; }
        .bar:hover { fill: orange; }
        .topic-list { list-style: none; padding: 0; text-align: left; }
        .topic-item { padding: 8px 10px; margin: 4px 0; background: #fff; border: 1px solid #ddd; border-radius: 4px; cursor: default; display: flex; justify-content: space-between; transition: all 0.2s; }
        .topic-item:hover { background-color: #fff3cd; border-color: #ffc107; transform: translateX(2px); box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
        .topic-prob { font-weight: bold; color: #555; background: #eee; padding: 2px 6px; border-radius: 10px; font-size: 0.8rem; }
        .sentiment-sentence { padding: 2px 0; border-radius: 3px; cursor: pointer; transition: background-color 0.2s ease, box-shadow 0.2s ease; }
        .sentiment-sentence:hover { text-decoration: underline; }

        /*  Removed !important to allow dynamic backgrounds  */
        .sentence-active { border-bottom: 3px solid #ffcc00; background-color: rgba(255, 204, 0, 0.1); }

        .sentence-selected { outline: 2px solid #007bff; background-color: rgba(0, 123, 255, 0.1); }
        .comment-box { font-style: italic; color: #666; margin: 10px 0; padding: 10px; border-left: 4px solid #007bff; background: #f0f7ff; }
        .content-wrapper { display: flex; flex-wrap: wrap; gap: 20px; }
        .video-column { flex: 1; min-width: 300px; position: sticky; top: 20px; height: fit-content; }
        .text-column { flex: 2; min-width: 300px; }
        video { width: 100%; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="/" class="brand">RedenPortal</a>
        <div class="nav-links"><a href="/">Start</a><a href="/abgeordnete">Abgeordnete</a><a href="/stats">Statistik</a></div>
    </nav>
    <div class="container">
        <a href="javascript:history.back()" class="back-link">&larr; Zurück</a>
        <div class="speech-content">
            <div style="text-align:center; margin-bottom:2rem;">
                <h1>Plenarprotokoll Details</h1>
                <#if redner??><p>Redner: <strong>${(redner.vollerName)!"Unbekannt"}</strong> (${(redner.fraktion)!"fraktionslos"})</p></#if>
            </div>
            <div style="text-align: center; margin-bottom: 10px;">
                <button id="resetFilterBtn" class="btn btn-secondary" onclick="resetCharts()" style="display:none;">Gesamtansicht wiederherstellen</button>
            </div>
            <div class="charts-container">
                <div class="chart-box"><h3 id="pos-title">POS Verteilung (Gesamt)</h3><div id="pos-chart"></div></div>
                <div class="chart-box"><h3 id="ne-title">Named Entities (Gesamt)</h3><div id="ne-chart"></div></div>
                <div class="chart-box">
                    <h3 id="topic-title">Top Topics (Gesamt)</h3>
                    <p style="font-size: 0.8em; color: #666; margin-bottom: 5px;">(Hovern zum Hervorheben)</p>
                    <ul class="topic-list" id="topic-list"></ul>
                </div>
            </div>
            <div style="margin-bottom: 15px;"><button id="toggleSentimentBtn" class="btn btn-outline-primary">Sentiment-Farben anzeigen</button></div>
            <div class="content-wrapper">
                <#if videoAvailable?? && videoAvailable>
                <div class="video-column">
                    <h3>Videoaufzeichnung</h3>
                    <video id="speechVideo" controls><source src="/api/video/${rede.id}" type="video/mp4"></video>
                </div>
                </#if>
                <div class="text-column" style="<#if !(videoAvailable?? && videoAvailable)>flex: 0 0 100%; max-width: 100%;</#if>">
                    <div class="speech-body">
                        <#list contentList as item>
                            <#if item.type == "paragraph">
                                <p class="speech-text" id="p-${item.index}">
                                    <#if item.sentences??>
                                        <#list item.sentences as s>
                                            <span class="sentiment-sentence" id="sent-${s.globalIndex!(-1)}" data-sentiment="${(s.sentiment!0)?c}" onclick="filterCharts(${s.globalIndex!(-1)?c})">${s.text}</span><#sep> </#sep>
                                        </#list>
                                    <#else>${item.text}</#if>
                                </p>
                            <#elseif item.type == "comment"><div class="comment-box">${item.text}</div></#if>
                        </#list>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <script>
        const globalPosData = JSON.parse('${posStatsJson?js_string}');
        const globalNeData = JSON.parse('${neStatsJson?js_string}');
        const topicData = JSON.parse('${topicStatsJson?js_string}');
        const sentencesData = JSON.parse('${sentencesJson?js_string}');

        const video = document.getElementById('speechVideo');
        if (video) {
            video.ontimeupdate = function() {
                const currentTime = video.currentTime;
                const duration = video.duration;
                if (!duration) return;
                const activeIndex = sentencesData.findIndex(function(s) { return currentTime >= s.relStart * duration && currentTime <= s.relEnd * duration; });
                $(".sentiment-sentence").removeClass("sentence-active");
                if (activeIndex !== -1) { const el = document.getElementById("sent-" + activeIndex); if (el) el.classList.add("sentence-active"); }
            };
        }

        let sentimentActive = false;
        document.getElementById('toggleSentimentBtn').addEventListener('click', function() {
            sentimentActive = !sentimentActive;
            const btn = this;
            if (sentimentActive) {
                btn.innerText = "Sentiment-Farben ausblenden";
                $(".sentiment-sentence").each(function() {
                    var score = parseFloat($(this).attr('data-sentiment'));
                    $(this).css("background-color", getColorForSentiment(score));
                });
            } else {
                btn.innerText = "Sentiment-Farben anzeigen";
                $(".sentiment-sentence").css("background-color", "transparent");
            }
        });

        // Continuous Sentiment coloring: Red -> White -> Green
        function getColorForSentiment(score) {
            score = Math.max(-1, Math.min(1, score));
            var r, g, b;
            if (score < 0) {
                // Negative: Red (255,0,0) to Neutral: White (255,255,255)
                var intensity = Math.round(255 * (1 + score));
                r = 255; g = intensity; b = intensity;
            } else {
                // Neutral: White (255,255,255) to Positive: Green (0,255,0)
                var intensity = Math.round(255 * (1 - score));
                r = intensity; g = 255; b = intensity;
            }
            return "rgb(" + r + ", " + g + ", " + b + ")";
        }

        function drawPosChart(dataMap) {
            d3.select("#pos-chart").selectAll("*").remove();
            const data = Object.entries(dataMap).map(function(e) { return {type: e[0], count: e[1]}; }).sort(function(a,b) { return b.count - a.count; }).slice(0, 10);
            if (data.length === 0) return;
            const m = {top: 10, right: 10, bottom: 40, left: 40}, w = 300-m.left-m.right, h = 200-m.top-m.bottom;
            const svg = d3.select("#pos-chart").append("svg").attr("width", 300).attr("height", 200).append("g").attr("transform", "translate(" + m.left + "," + m.top + ")");
            const x = d3.scaleBand().range([0, w]).padding(0.1).domain(data.map(function(d) { return d.type; }));
            const y = d3.scaleLinear().range([h, 0]).domain([0, d3.max(data, function(d) { return d.count; })]);
            svg.append("g").attr("transform", "translate(0," + h + ")").call(d3.axisBottom(x));
            svg.append("g").call(d3.axisLeft(y).ticks(5));
            svg.selectAll(".bar").data(data).enter().append("rect").attr("class", "bar").attr("x", function(d) { return x(d.type); }).attr("y", function(d) { return y(d.count); }).attr("width", x.bandwidth()).attr("height", function(d) { return h - y(d.count); });
        }

        function drawNeChart(dataMap) {
            d3.select("#ne-chart").selectAll("*").remove();
            const data = Object.entries(dataMap).map(function(e) { return {name: e[0], count: e[1]}; });
            if (data.length === 0) return;
            const svg = d3.select("#ne-chart").append("svg").attr("width", 300).attr("height", 200);
            const root = d3.hierarchy({children: data}).sum(function(d) { return d.count; });
            const nodes = d3.pack().size([300, 200]).padding(2)(root).leaves();
            const color = d3.scaleOrdinal(d3.schemeCategory10);
            const g = svg.selectAll("g").data(nodes).enter().append("g").attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
            g.append("circle").attr("r", function(d) { return d.r; }).attr("fill", function(d,i) { return color(i); });
            g.append("text").attr("dy", ".3em").style("text-anchor", "middle").style("font-size", "10px").text(function(d) { return d.r > 15 ? d.data.name.substring(0, 5) : ""; });
        }

        function drawTopics(list) {
            const tList = $("#topic-list").empty();
            if (!list || list.length === 0) { tList.append("<li>Keine Topics</li>"); return; }
            list.slice(0, 5).forEach(function(t) {
                const probValue = t.probability !== undefined ? t.probability : (t.score !== undefined ? t.score : 0);
                const prob = (probValue * 100).toFixed(1);
                const li = $("<li class='topic-item'><span>" + t.name + "</span><span class='topic-prob'>" + prob + "%</span></li>");
                li.on("mouseenter", function() { highlightTopic(t.name); }).on("mouseleave", resetHighlights);
                tList.append(li);
            });
        }

        function highlightTopic(topicName) {
            if (sentimentActive) return;
            resetHighlights();
            sentencesData.forEach(function(sent) {
                const match = (sent.topics || []).find(function(t) { return t.name === topicName; });
                if (match) {
                    const el = document.getElementById("sent-" + sent.globalIndex);
                    if (el) {
                        const score = match.score !== undefined ? match.score : (match.probability || 0);
                        let op = Math.min(1.0, score * 1.5); if (op < 0.2) op = 0.2;
                        el.style.backgroundColor = "rgba(255, 193, 7, " + op + ")";
                        el.style.boxShadow = "0 0 4px rgba(255, 193, 7, " + op + ")";
                    }
                }
            });
        }

        function resetHighlights() { if (!sentimentActive) $(".sentiment-sentence").css({"background-color": "transparent", "box-shadow": "none"}); }

        function filterCharts(index) {
            if (index < 0 || index >= sentencesData.length) return;
            const s = sentencesData[index];
            $(".sentiment-sentence").removeClass("sentence-selected");
            $("#sent-" + index).addClass("sentence-selected");
            $("#pos-title").text("POS (Satz " + (index + 1) + ")");
            $("#ne-title").text("NE (Satz " + (index + 1) + ")");
            $("#topic-title").text("Topics (Satz " + (index + 1) + ")");
            $("#resetFilterBtn").show();
            drawPosChart(s.posStats || {}); drawNeChart(s.neStats || {}); drawTopics(s.topics || []);
        }

        function resetCharts() {
            $(".sentiment-sentence").removeClass("sentence-selected");
            $("#pos-title").text("POS Verteilung (Gesamt)");
            $("#ne-title").text("Named Entities (Gesamt)");
            $("#topic-title").text("Top Topics (Gesamt)");
            $("#resetFilterBtn").hide();
            drawPosChart(globalPosData); drawNeChart(globalNeData); drawTopics(topicData);
        }

        $(document).ready(resetCharts);
    </script>
</body>
</html>