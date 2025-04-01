const express = require('express');
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(express.json({ limit: '1mb' }));

const mermaidJs = fs.readFileSync(path.join(__dirname, 'mermaid.min.js'), 'utf8');

let browser;
let page;

// Launch browser and single tab on startup
(async () => {
    browser = await puppeteer.launch({
        args: ['--no-sandbox', '--disable-setuid-sandbox'],
        headless: 'new'
    });
    page = await browser.newPage();

    // Optional: log from browser console
    page.on('console', msg => {
        for (let i = 0; i < msg.args().length; ++i) {
            msg.args()[i].jsonValue().then(val => console.log('[browser log]', val));
        }
    });

    console.log("Puppeteer browser launched and page initialized.");
})();

// Health check
app.get('/ping', (req, res) => {
    res.status(200).send('Hello from Mermaid SVG microservice!');
});

// SVG generation endpoint
app.post('/generate-svg', async (req, res) => {
    const { code } = req.body;
    if (!code) return res.status(400).json({ error: "Mermaid code required." });

    try {
        await page.goto('about:blank');
        await page.setContent(`
            <!DOCTYPE html>
            <html>
            <head>
              <script>${mermaidJs}</script>
            </head>
            <body>
              <div id="container">Loading...</div>
              <script>
                mermaid.initialize({ startOnLoad: false });
                const diagram = ${JSON.stringify(code)};
                console.log("Mermaid diagram received:");
                console.log(diagram);

                mermaid.render("generated", diagram).then(({ svg }) => {
                    document.getElementById("container").innerHTML = svg;
                    window.__MERMAID_RENDERED__ = true;
                }).catch(err => {
                    console.error("Mermaid render error:", err);
                    document.getElementById("container").innerHTML = "<svg><text x='10' y='20'>Render error</text></svg>";
                    window.__MERMAID_RENDERED__ = true;
                });
              </script>
            </body>
            </html>
        `, { waitUntil: 'networkidle0' });

        await page.waitForFunction('window.__MERMAID_RENDERED__ === true');
        const svg = await page.$eval('#container', el => el.innerHTML);

        res.set('Content-Type', 'image/svg+xml');
        res.send(svg);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Failed to render Mermaid diagram.' });
    }
});

// shutdown
const shutdown = async () => {
    console.log("Shutting down Puppeteer...");
    if (browser) await browser.close();
    process.exit();
};

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => console.log(`Mermaid SVG microservice running on port ${PORT}`));
