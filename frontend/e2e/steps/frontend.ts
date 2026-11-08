import { After, AfterAll, Before, Given, When, Then, setDefaultTimeout } from '@cucumber/cucumber'
import { chromium, type Browser, type Page } from 'playwright'
import { strict as assert } from 'node:assert'

setDefaultTimeout(30_000)
let browser: Browser | undefined
let page: Page
const baseURL = process.env.FRONTEND_URL ?? 'http://localhost:5173'

Before(async () => {
  browser ??= await chromium.launch({ headless: true })
  page = await browser.newPage()
})
After(async () => { await page?.close() })
AfterAll(async () => { await browser?.close() })

Given('the frontend dashboard is open', async () => {
  await page.goto(baseURL)
  await page.getByRole('heading', { name: 'Your stream, in view.' }).waitFor()
})
When('I choose Spanish', async () => { await page.locator('#language-select').selectOption('es') })
When('I choose the light theme', async () => { await page.locator('#theme-select').selectOption('light') })
When('I reload the page', async () => { await page.reload() })
Then('the dashboard is shown in Spanish', async () => {
  await page.getByRole('heading', { name: 'Tu directo, de un vistazo.' }).waitFor()
  assert.equal(await page.locator('#language-select').inputValue(), 'es')
})
Then('the light theme is active', async () => {
  assert.equal(await page.locator('#theme-select').inputValue(), 'light')
  await page.locator('.v-application.v-theme--light').waitFor()
})
When('I open the OBS overlay', async () => { await page.getByRole('link', { name: 'OBS overlay', exact: true }).click() })
Then('the local development overlay is shown', async () => {
  await page.getByText('Waiting for live chat messages…', { exact: true }).waitFor()
  assert.equal(new URL(page.url()).pathname, '/embed/chat')
})
When('I return after Twitch authorization', async () => { await page.goto(`${baseURL}/?twitch=connected`) })
Then('the dashboard confirms the Twitch connection', async () => {
  await page.getByText('Connected', { exact: true }).waitFor()
})
