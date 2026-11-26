(() => {
  const STORAGE_KEY = "wayfarer-campaign-content-v1";
  const results = document.querySelector("[data-library-results]");
  if (!results) return;
  const filterControls = [...document.querySelectorAll("[data-library-filter]")];
  const search = document.querySelector("[data-library-search]");
  const count = document.querySelector("[data-library-count]");
  let items = [];

  const readItems = () => {
    try {
      const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
      return Array.isArray(value) ? value.filter((item) => item && item.status === "Published") : [];
    } catch {
      return [];
    }
  };
  const makeElement = (tag, className, value) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (value !== undefined) node.textContent = value;
    return node;
  };
  const selected = (key) => document.querySelector(`[data-library-filter="${key}"]`).value;
  const filteredItems = () => {
    const query = search.value.trim().toLowerCase();
    return items.filter((item) => {
      const match = (key, value) => !selected(key) || item[key] === value;
      return match("campaign", item.campaign) && match("product", item.product) && match("typeLabel", item.typeLabel) && match("language", item.language)
        && (!query || `${item.campaign} ${item.product} ${item.typeLabel} ${item.language} ${item.content}`.toLowerCase().includes(query));
    });
  };
  const populateFilter = (control, values) => {
    const keep = control.value;
    const first = control.options[0];
    control.replaceChildren(first);
    [...new Set(values)].sort().forEach((value) => {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = value;
      control.append(option);
    });
    if ([...control.options].some((option) => option.value === keep)) control.value = keep;
  };
  const render = () => {
    items = readItems().sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
    populateFilter(document.querySelector('[data-library-filter="campaign"]'), items.map((item) => item.campaign));
    populateFilter(document.querySelector('[data-library-filter="product"]'), items.map((item) => item.product));
    populateFilter(document.querySelector('[data-library-filter="type"]'), items.map((item) => item.typeLabel));
    populateFilter(document.querySelector('[data-library-filter="language"]'), items.map((item) => item.language));
    const visible = filteredItems();
    count.textContent = `${visible.length} ${visible.length === 1 ? "published item" : "published items"}`;
    results.replaceChildren();
    if (!items.length) {
      const empty = makeElement("div", "ai-empty-state");
      empty.append(makeElement("strong", "", "No campaign content is available yet"), makeElement("span", "", "When a tenant publishes campaign content, it will appear here with its product and campaign details."));
      const browse = makeElement("a", "button secondary", "Explore campaign marketplace");
      browse.href = "campaign-marketplace.html";
      const tenant = makeElement("a", "library-tenant-link", "Open the tenant content setup in this demo");
      tenant.href = "../tenant/ai-content.html";
      empty.append(browse, tenant);
      results.append(empty);
      return;
    }
    if (!visible.length) {
      const empty = makeElement("div", "ai-empty-state");
      empty.append(makeElement("strong", "", "No content matches these filters"), makeElement("span", "", "Adjust or clear your filters to see other published assets."));
      results.append(empty);
      return;
    }
    const grid = makeElement("div", "library-grid");
    visible.forEach((item) => {
      const card = makeElement("article", "panel library-card");
      card.dataset.row = "";
      const top = makeElement("div", "library-card-top");
      const type = makeElement("span", "badge active", item.typeLabel);
      const language = makeElement("span", "library-language", item.language);
      top.append(type, language);
      const title = makeElement("h2", "", item.campaign);
      const product = makeElement("p", "library-product", item.product);
      const preview = makeElement("p", "library-copy", item.content);
      const actions = makeElement("div", "library-actions");
      const copy = makeElement("button", "button", "Copy content");
      copy.type = "button";
      copy.dataset.libraryCopy = item.id;
      const campaign = makeElement("a", "button secondary", "Campaign details");
      campaign.href = `campaign-detail.html?campaign=${encodeURIComponent(item.campaignId)}`;
      const tracking = makeElement("a", "library-tracking-link", "Open tracking links →");
      tracking.href = "tracking-links.html";
      actions.append(copy, campaign);
      card.append(top, title, product, preview, actions, tracking);
      grid.append(card);
    });
    results.append(grid);
  };
  const copyContent = async (item) => {
    const button = document.querySelector(`[data-library-copy="${CSS.escape(item.id)}"]`);
    try {
      await navigator.clipboard.writeText(item.content);
      button.textContent = "Copied";
    } catch {
      const helper = document.createElement("textarea");
      helper.value = item.content;
      helper.style.position = "fixed";
      helper.style.opacity = "0";
      document.body.append(helper);
      helper.select();
      const copied = document.execCommand("copy");
      helper.remove();
      button.textContent = copied ? "Copied" : "Select to copy";
    }
    window.setTimeout(() => { if (button.isConnected) button.textContent = "Copy content"; }, 1600);
  };
  results.addEventListener("click", (event) => {
    const button = event.target.closest("[data-library-copy]");
    if (!button) return;
    const item = items.find((entry) => entry.id === button.dataset.libraryCopy);
    if (item) copyContent(item);
  });
  filterControls.forEach((control) => control.addEventListener("change", render));
  search.addEventListener("input", render);
  window.addEventListener("storage", (event) => { if (event.key === STORAGE_KEY) render(); });
  render();
})();
