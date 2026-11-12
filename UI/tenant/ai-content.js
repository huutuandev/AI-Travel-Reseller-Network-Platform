(() => {
  const STORAGE_KEY = "wayfarer-campaign-content-v1";
  const products = {
    halong: { product: "Ha Long Bay cruise", campaign: "Ha Long Bay Weekend Escape", destination: "Ha Long Bay" },
    hoian: { product: "Hoi An stay", campaign: "Hoi An Lantern Season", destination: "Hoi An" },
    phuquoc: { product: "Phu Quoc beach stay", campaign: "Phu Quoc Island Escape", destination: "Phu Quoc" },
  };
  const labels = {
    caption: "Social media caption", description: "Promotional description", hashtags: "Hashtag suggestions", cta: "Call to action", video: "Video script",
  };
  const form = document.querySelector("[data-tenant-content-form]");
  if (!form) return;
  const productSelect = document.querySelector("#tenant-ai-product");
  const campaignSelect = document.querySelector("#tenant-ai-campaign");
  const typeSelect = document.querySelector("#tenant-ai-type");
  const languageSelect = document.querySelector("#tenant-ai-language");
  const toneSelect = document.querySelector("#tenant-ai-tone");
  const output = document.querySelector("#tenant-ai-output");
  const empty = document.querySelector("[data-tenant-ai-empty]");
  const error = document.querySelector("[data-tenant-ai-error]");
  const feedback = document.querySelector("[data-tenant-ai-feedback]");
  const status = document.querySelector("[data-tenant-ai-status]");
  const history = document.querySelector("[data-ai-history]");
  const count = document.querySelector("[data-ai-history-count]");
  const buttons = Object.fromEntries([...document.querySelectorAll("[data-ai-action]")].map((button) => [button.dataset.aiAction, button]));
  let draft = null;
  let generationTimer;

  const readItems = () => {
    try {
      const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
      return Array.isArray(value) ? value : [];
    } catch {
      return [];
    }
  };
  const writeItems = (items) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
      return true;
    } catch {
      feedback.textContent = "Browser storage is unavailable. Open this prototype through Live Server to share content between pages.";
      return false;
    }
  };
  const selectedProduct = () => products[productSelect.value];
  const isBriefReady = () => Boolean(productSelect.value && campaignSelect.value && productSelect.value === campaignSelect.value);
  const syncBrief = () => {
    const hasBoth = Boolean(productSelect.value && campaignSelect.value);
    empty.hidden = hasBoth;
    error.hidden = true;
    if (hasBoth && productSelect.value !== campaignSelect.value) {
      error.textContent = "Choose the campaign that matches the selected product.";
      error.hidden = false;
    }
    buttons.generate.disabled = !isBriefReady();
  };
  const mockContent = () => {
    const item = selectedProduct();
    const campaign = item.campaign;
    const type = typeSelect.value;
    const language = languageSelect.value;
    const tone = toneSelect.value.toLowerCase();
    if (language === "Vietnamese") {
      const vn = {
        halong: "Khám phá Vịnh Hạ Long theo nhịp riêng của bạn: làn nước xanh, những dãy núi đá vôi kỳ vĩ và một kỳ nghỉ cuối tuần thật đáng nhớ.",
        hoian: "Khi phố lên đèn lồng, Hội An trở nên thật khác. Chọn một chốn nghỉ bên sông và thong thả tận hưởng vẻ đẹp phố cổ.",
        phuquoc: "Nắng vàng, biển xanh và những buổi chiều không vội. Phú Quốc đang chờ bạn cho một kỳ nghỉ trên đảo thật trọn vẹn.",
      }[productSelect.value];
      if (type === "hashtags") return "#DuLichVietNam #" + item.destination.replaceAll(" ", "") + " #KyNghiDangNho #TravelWithUs";
      if (type === "cta") return "Khám phá " + item.destination + " cùng " + campaign + " — chọn ngày khởi hành của bạn ngay hôm nay.";
      if (type === "video") return `[Cảnh mở đầu] ${item.destination}\n[Trải nghiệm] ${vn}\n[Lời kết] Khám phá ${campaign} cùng thương hiệu của bạn.`;
      if (type === "description") return vn + " Chương trình " + campaign + " được tuyển chọn để bạn tận hưởng điểm đến theo cách riêng, với sự đồng hành từ đối tác du lịch địa phương.";
      return vn + " Lên kế hoạch cho hành trình tiếp theo cùng " + campaign + ". #" + item.destination.replaceAll(" ", "");
    }
    const lead = {
      halong: "Sail through emerald water beneath the limestone peaks of Ha Long Bay.",
      hoian: "Follow the lantern light to a slower evening beside the Hoi An river.",
      phuquoc: "Trade the rush for clear water, soft sand and long island afternoons in Phu Quoc.",
    }[productSelect.value];
    const invitation = tone.includes("premium") ? "A considered escape, thoughtfully arranged." : tone.includes("playful") ? "Your next little adventure is calling." : "Make room for a journey worth remembering.";
    if (type === "hashtags") return `#${item.destination.replaceAll(" ", "")} #TravelVietnam #${campaign.replaceAll(" ", "")} #TravelWell`;
    if (type === "cta") return `Explore ${campaign} and start planning your ${item.destination} getaway today.`;
    if (type === "video") return `[Opening shot: ${item.destination}]\n${lead}\n[Experience: local details and scenery]\n${invitation}\n[End frame: ${campaign} · Discover the journey]`;
    if (type === "description") return `${lead} ${campaign} pairs a locally hosted experience with the time and space to discover the destination at your own pace. ${invitation}`;
    return `${lead} ${invitation} Discover ${campaign} with a local travel partner. #${item.destination.replaceAll(" ", "")} #TravelVietnam`;
  };
  const renderHistory = () => {
    const items = readItems().sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
    count.textContent = `${items.length} ${items.length === 1 ? "item" : "items"}`;
    if (!items.length) {
      history.innerHTML = '<div class="ai-empty-state"><strong>No content saved yet</strong><span>Generate a sample, then save it as a draft or publish it for resellers.</span></div>';
      return;
    }
    history.replaceChildren();
    const list = document.createElement("div");
    list.className = "ai-history-list";
    items.forEach((item) => {
      const card = document.createElement("article");
      card.className = "ai-history-item";
      const details = document.createElement("div");
      const heading = document.createElement("strong");
      heading.textContent = `${item.typeLabel} · ${item.campaign}`;
      const meta = document.createElement("span");
      meta.textContent = `${item.product} · ${item.language} · ${new Date(item.updatedAt).toLocaleString()}`;
      const preview = document.createElement("p");
      preview.textContent = item.content;
      details.append(heading, meta, preview);
      const badge = document.createElement("span");
      badge.className = `badge ${item.status === "Published" ? "approved" : "pending"}`;
      badge.textContent = item.status;
      card.append(details, badge);
      list.append(card);
    });
    history.append(list);
  };
  const updateDraftStatus = (value, message) => {
    status.textContent = value;
    status.className = `badge ${value === "Published" ? "approved" : value === "Draft" ? "pending" : ""}`;
    feedback.textContent = message;
  };
  const copyDraft = async () => {
    if (!output.value.trim()) {
      feedback.textContent = "Generate or enter content before copying.";
      return;
    }
    try {
      await navigator.clipboard.writeText(output.value);
    } catch {
      output.focus();
      output.select();
      if (!document.execCommand("copy")) {
        feedback.textContent = "Select the content and copy it with your keyboard.";
        return;
      }
    }
    feedback.textContent = "Content copied to clipboard.";
  };
  const persistDraft = (publish) => {
    if (!draft || !output.value.trim()) {
      feedback.textContent = "Generate or enter content before saving.";
      return;
    }
    const items = readItems();
    const record = { ...draft, content: output.value.trim(), status: publish ? "Published" : "Draft", updatedAt: new Date().toISOString() };
    const index = items.findIndex((item) => item.id === record.id);
    if (index < 0) items.unshift(record);
    else items[index] = record;
    if (!writeItems(items)) return;
    draft = record;
    updateDraftStatus(record.status, publish ? "Published to the Reseller Content Library." : "Draft saved and associated with this campaign.");
    renderHistory();
  };

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    syncBrief();
    if (!isBriefReady()) {
      error.hidden = false;
      error.textContent = productSelect.value && campaignSelect.value ? "Choose the campaign that matches the selected product." : "Select both a product and campaign before generating content.";
      feedback.textContent = "Complete the content brief to continue.";
      return;
    }
    const button = buttons.generate;
    const original = button.textContent;
    button.disabled = true;
    button.textContent = "Preparing sample…";
    feedback.textContent = "Preparing illustrative campaign copy…";
    clearTimeout(generationTimer);
    generationTimer = setTimeout(() => {
      const product = selectedProduct();
      draft = {
        id: crypto.randomUUID ? crypto.randomUUID() : `content-${Date.now()}`,
        productId: productSelect.value,
        product: product.product,
        campaignId: campaignSelect.value,
        campaign: product.campaign,
        type: typeSelect.value,
        typeLabel: labels[typeSelect.value],
        language: languageSelect.value,
        tone: toneSelect.value,
        content: mockContent(),
        status: "Draft",
        updatedAt: new Date().toISOString(),
      };
      output.value = draft.content;
      output.disabled = false;
      buttons.copy.disabled = false;
      buttons.save.disabled = false;
      buttons.publish.disabled = false;
      button.textContent = original;
      button.disabled = false;
      updateDraftStatus("Draft", "Sample content generated. Edit it, save a draft or publish it to resellers.");
    }, 350);
  });
  productSelect.addEventListener("change", syncBrief);
  campaignSelect.addEventListener("change", syncBrief);
  buttons.copy.addEventListener("click", copyDraft);
  buttons.save.addEventListener("click", () => persistDraft(false));
  buttons.publish.addEventListener("click", () => persistDraft(true));
  window.addEventListener("storage", (event) => { if (event.key === STORAGE_KEY) renderHistory(); });
  syncBrief();
  renderHistory();
})();
