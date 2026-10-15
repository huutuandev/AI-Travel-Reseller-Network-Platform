(() => {
  const toast = (message) => {
    let node = document.querySelector("[data-toast]");
    if (!node) {
      node = document.createElement("div");
      node.dataset.toast = "";
      node.className = "toast";
      node.setAttribute("role", "status");
      document.body.append(node);
    }
    node.textContent = message;
    node.classList.add("visible");
    clearTimeout(node.hideTimer);
    node.hideTimer = setTimeout(() => node.classList.remove("visible"), 2400);
  };

  const updateRows = () => {
    const queries = [...document.querySelectorAll("[data-search]")]
      .map((input) => input.value.trim().toLowerCase()).filter(Boolean);
    const filters = [...document.querySelectorAll("[data-filter]")]
      .map((select) => ({
        type: select.dataset.filter,
        value: (select.selectedOptions[0]?.value || select.selectedOptions[0]?.textContent || "").trim().toLowerCase(),
      }))
      .filter(({ value }) => value && !/^(all|any|last \d+ days?|this month)/i.test(value));

    document.querySelectorAll("[data-row]").forEach((row) => {
      const text = row.textContent.toLowerCase();
      const filterMatch = filters.some(({ type, value }) => {
        if (type === "commission" && value.startsWith("10+")) {
          const rate = text.match(/(\d+(?:\.\d+)?)\s*%/);
          return !rate || Number(rate[1]) < 10;
        }
        return !text.includes(value);
      });
      row.hidden = queries.some((query) => !text.includes(query)) || filterMatch;
    });
  };

  const productDialog = document.querySelector("#product-dialog");
  const productForm = document.querySelector("[data-product-form]");
  const clearProductErrors = () => {
    productForm?.querySelectorAll("[data-error-for]").forEach((node) => {
      node.textContent = "";
      node.classList.remove("field-error");
    });
    const summary = productForm?.querySelector("[data-product-error]");
    if (summary) { summary.hidden = true; summary.textContent = ""; }
    productForm?.querySelectorAll("[aria-invalid='true']").forEach((field) => field.removeAttribute("aria-invalid"));
  };
  const closeProductDialog = () => {
    if (productDialog?.open) productDialog.close();
    productForm?.reset();
    clearProductErrors();
  };

  const campaignSlider = document.querySelector("[data-campaign-slider]");
  const featuredCampaigns = [
    { image: "assets/images/halong-bay.svg", alt: "Illustration of limestone islands at Ha Long Bay", title: "Ha Long Bay Weekend Escape", region: "VIETNAM", product: "Ha Long Bay cruise", dates: "01 Oct 2026 — 31 Dec 2026", commission: "12% commission", stamp: "12%" },
    { image: "assets/images/hoi-an.svg", alt: "Illustration of lanterns along a riverside street in Hoi An", title: "Hoi An Lantern Season", region: "VIETNAM", product: "Hoi An stay", dates: "15 Oct 2026 — 31 Jan 2027", commission: "10% commission", stamp: "10%" },
    { image: "assets/images/phu-quoc.svg", alt: "Illustration of a palm-lined beach on Phu Quoc island", title: "Phu Quoc Island Escape", region: "VIETNAM", product: "Phu Quoc beach stay", dates: "14 Oct 2026 — 28 Feb 2027", commission: "$38 / booking", stamp: "$38" },
  ];
  let activeCampaign = 0;
  let sliderTimer;
  let campaignSliderPaused = false;
  const renderCampaign = (index) => {
    if (!campaignSlider) return;
    activeCampaign = (index + featuredCampaigns.length) % featuredCampaigns.length;
    const campaign = featuredCampaigns[activeCampaign];
    const image = campaignSlider.querySelector("[data-slider-image]");
    image.src = campaign.image;
    image.alt = campaign.alt;
    campaignSlider.querySelector("[data-slider-title]").textContent = campaign.title;
    campaignSlider.querySelector("[data-slider-region]").textContent = campaign.region;
    campaignSlider.querySelector("[data-slider-product]").textContent = campaign.product;
    campaignSlider.querySelector("[data-slider-dates]").textContent = campaign.dates;
    campaignSlider.querySelector("[data-slider-commission]").textContent = campaign.commission;
    campaignSlider.querySelector("[data-slider-stamp]").textContent = campaign.stamp;
    campaignSlider.querySelectorAll("[data-slider-dot]").forEach((dot, dotIndex) => {
      dot.setAttribute("aria-current", String(dotIndex === activeCampaign));
    });
  };
  const stopCampaignSlider = () => clearInterval(sliderTimer);
  const startCampaignSlider = () => {
    stopCampaignSlider();
    if (!campaignSlider || campaignSliderPaused || featuredCampaigns.length < 2 || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    sliderTimer = setInterval(() => renderCampaign(activeCampaign + 1), 6000);
  };
  if (campaignSlider) {
    campaignSlider.querySelectorAll("[data-slider-control][data-index]").forEach((dot) => dot.dataset.sliderDot = "");
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) campaignSlider.querySelector("[data-slider-toggle]").hidden = true;
    startCampaignSlider();
    campaignSlider.addEventListener("pointerenter", stopCampaignSlider);
    campaignSlider.addEventListener("pointerleave", startCampaignSlider);
    campaignSlider.addEventListener("focusin", stopCampaignSlider);
    campaignSlider.addEventListener("focusout", (event) => {
      if (!campaignSlider.contains(event.relatedTarget)) startCampaignSlider();
    });
    document.addEventListener("visibilitychange", () => document.hidden ? stopCampaignSlider() : startCampaignSlider());
  }

  const campaignStart = document.querySelector("#campaign-start");
  const campaignEnd = document.querySelector("#campaign-end");
  if (campaignStart && campaignEnd) {
    campaignEnd.min = campaignStart.value;
    campaignStart.max = campaignEnd.value;
  }

  document.addEventListener("click", async (event) => {
    const addProductButton = event.target.closest("[data-add-product]");
    if (addProductButton && productDialog) {
      productDialog.showModal();
      productDialog.querySelector("#product-name")?.focus();
      return;
    }

    if (event.target.closest("[data-product-close]")) {
      closeProductDialog();
      return;
    }

    const sliderControl = event.target.closest("[data-slider-control]");
    if (sliderControl && campaignSlider) {
      if (sliderControl.hasAttribute("data-slider-toggle")) {
        campaignSliderPaused = !campaignSliderPaused;
        sliderControl.setAttribute("aria-pressed", String(campaignSliderPaused));
        sliderControl.setAttribute("aria-label", campaignSliderPaused ? "Play slideshow" : "Pause slideshow");
        sliderControl.innerHTML = campaignSliderPaused
          ? '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m8 5 11 7-11 7z"/></svg>'
          : '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5h3v14H8zM15 5h3v14h-3z"/></svg>';
        if (campaignSliderPaused) stopCampaignSlider();
        else startCampaignSlider();
        return;
      }
      if (sliderControl.hasAttribute("data-index")) renderCampaign(Number(sliderControl.dataset.index));
      else renderCampaign(activeCampaign + (sliderControl.dataset.direction === "previous" ? -1 : 1));
      startCampaignSlider();
      return;
    }

    const menu = event.target.closest("[data-menu]");
    if (menu) {
      const sidebar = document.querySelector(".sidebar");
      sidebar?.classList.toggle("open");
      menu.setAttribute("aria-expanded", String(sidebar?.classList.contains("open")));
      return;
    }

    const passwordToggle = event.target.closest("[data-password]");
    if (passwordToggle) {
      const input = document.querySelector(passwordToggle.dataset.password);
      if (input) {
        input.type = input.type === "password" ? "text" : "password";
        passwordToggle.textContent = input.type === "password" ? "Show" : "Hide";
        passwordToggle.setAttribute("aria-pressed", String(input.type === "text"));
      }
      return;
    }

    const copyButton = event.target.closest("[data-copy]");
    if (copyButton) {
      const value = copyButton.dataset.copy;
      try {
        if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(value);
        else {
          const helper = document.createElement("textarea");
          helper.value = value;
          helper.style.position = "fixed";
          helper.style.opacity = "0";
          document.body.append(helper);
          helper.select();
          const copied = document.execCommand("copy");
          helper.remove();
          if (!copied) throw new Error("Copy unavailable");
        }
        copyButton.textContent = "Copied";
        copyButton.setAttribute("aria-label", "Tracking link copied");
        setTimeout(() => { copyButton.textContent = "Copy link"; }, 1500);
      } catch {
        toast("Copy is unavailable here. Select the tracking URL to copy it.");
      }
      return;
    }

    const joinButton = event.target.closest("[data-join]");
    if (joinButton) {
      const status = document.querySelector("[data-join-status]");
      if (status) status.textContent = "Pending Approval · Demo request";
      joinButton.textContent = "Application pending";
      joinButton.disabled = true;
      return;
    }

    const statusButton = event.target.closest("[data-toggle-status]");
    if (statusButton) {
      const status = document.querySelector("[data-campaign-status]");
      if (status) {
        const paused = status.textContent.trim().toLowerCase() === "active";
        status.textContent = paused ? "Paused" : "Active";
        status.className = `badge ${paused ? "paused" : "active"}`;
        statusButton.textContent = paused ? "Resume campaign" : "Pause campaign";
        toast(`Campaign status changed to ${paused ? "Paused" : "Active"} for this demo.`);
      }
      return;
    }

    const tenantToggle = event.target.closest("[data-tenant-toggle]");
    if (tenantToggle) {
      const row = tenantToggle.closest("tr");
      const badge = row?.querySelector("[data-tenant-status]") || document.querySelector("[data-tenant-status]");
      if (badge) {
        const next = badge.textContent.trim() === "Active" ? "Suspended" : "Active";
        badge.textContent = next;
        badge.className = `badge ${next === "Active" ? "active" : "paused"}`;
        tenantToggle.textContent = next === "Active" ? "Suspend" : "Activate";
        toast(`Tenant marked ${next.toLowerCase()} for this demo.`);
      }
      return;
    }

    const conversionAction = event.target.closest("[data-conversion-action]");
    if (conversionAction) {
      const row = conversionAction.closest("tr");
      const badge = row?.querySelector("[data-review-status]");
      if (badge?.textContent.trim() === "Pending") {
        const approved = conversionAction.dataset.conversionAction === "approve";
        badge.textContent = approved ? "Approved" : "Rejected";
        badge.className = `badge ${approved ? "approved" : "paused"}`;
        row.querySelectorAll("[data-conversion-action]").forEach((button) => { button.disabled = true; button.textContent = "Reviewed"; });
        const count = document.querySelector("[data-pending-count]");
        if (count) count.textContent = String(Math.max(0, Number(count.textContent) - 1));
        toast(`Sample conversion ${approved ? "approved" : "rejected"}.`);
      }
      return;
    }

    const aiDecision = event.target.closest("[data-ai-decision]");
    if (aiDecision) {
      const row = aiDecision.closest("[data-ai-review-row]");
      const badge = row?.querySelector("[data-ai-status]");
      if (badge?.textContent.trim() === "Pending review") {
        const approved = aiDecision.dataset.aiDecision === "approve";
        badge.textContent = approved ? "Approved" : "Changes requested";
        badge.className = `badge ${approved ? "approved" : "pending"}`;
        row.querySelectorAll("[data-ai-decision]").forEach((button) => { button.disabled = true; button.textContent = "Reviewed"; });
        toast(approved ? "Sample content approved." : "Changes requested for sample content.");
      }
      return;
    }

    if (event.target.closest("[data-mark-read]")) {
      document.querySelectorAll(".badge.unread").forEach((badge) => {
        badge.textContent = "Read";
        badge.className = "badge read";
      });
      document.querySelectorAll(".top-notice .badge.pending, .sidebar a[href$='notifications.html'] .badge.pending").forEach((badge) => {
        badge.textContent = "0";
      });
      toast("Unread notifications marked as read for this demo.");
      return;
    }

    const placeholderLink = event.target.closest('a[href="#"]');
    if (placeholderLink) {
      event.preventDefault();
      toast("This action is a visual demo and is not connected to an account.");
      return;
    }

    const genericButton = event.target.closest("button:not([type=submit]):not([data-menu]):not([data-password]):not([data-copy]):not([data-join]):not([data-mark-read]):not([data-add-product]):not([data-product-close]):not([data-tenant-toggle]):not([data-conversion-action]):not([data-ai-decision]):not([data-ai-copy]):not([data-ai-submit]):not([data-ai-action]):not([data-library-copy])");
    if (genericButton && !genericButton.disabled && !genericButton.form) toast("This control is a visual demo and is not connected to an account.");
  });

  document.addEventListener("input", (event) => {
    if (event.target.matches("[data-search]")) updateRows();
  });
  document.addEventListener("change", (event) => {
    if (event.target.matches("[data-filter]")) updateRows();
    if (event.target.id === "commission-type") {
      const suffix = event.target.form?.querySelector("[data-commission-suffix]");
      if (suffix) suffix.textContent = event.target.value === "fixed" ? "$" : "%";
    }
    if (event.target.id === "campaign-start" || event.target.id === "campaign-end") {
      const form = event.target.form;
      const start = form?.querySelector("#campaign-start");
      const end = form?.querySelector("#campaign-end");
      if (start && end) {
        end.min = start.value;
        start.max = end.value;
      }
    }
  });
  document.addEventListener("submit", (event) => {
    if (event.target.matches("[data-product-form]")) {
      event.preventDefault();
      clearProductErrors();
      const formData = new FormData(event.target);
      const product = Object.fromEntries(formData.entries());
      const errors = {};
      for (const key of ["name", "category", "destination", "description", "price", "currency", "status"]) {
        if (!String(product[key] || "").trim()) errors[key] = "This field is required.";
      }
      const price = Number(product.price);
      if (product.price && (!Number.isFinite(price) || price <= 0)) errors.price = "Enter a price greater than zero.";

      const firstInvalid = Object.keys(errors)[0];
      if (firstInvalid) {
        for (const [key, message] of Object.entries(errors)) {
          const field = productForm.elements.namedItem(key);
          const messageNode = productForm.querySelector(`[data-error-for="${key}"]`);
          field?.setAttribute("aria-invalid", "true");
          if (messageNode) { messageNode.textContent = message; messageNode.classList.add("field-error"); }
        }
        const summary = productForm.querySelector("[data-product-error]");
        summary.textContent = "Please correct the highlighted fields and try again.";
        summary.hidden = false;
        productForm.elements.namedItem(firstInvalid)?.focus();
        return;
      }

      const row = document.createElement("tr");
      row.dataset.row = "";
      const nameCell = document.createElement("td");
      const name = document.createElement("strong");
      name.textContent = product.name.trim();
      const destination = document.createElement("span");
      destination.className = "row-meta";
      destination.textContent = product.destination.trim();
      nameCell.append(name, destination);
      const categoryCell = document.createElement("td");
      categoryCell.textContent = product.category;
      const priceCell = document.createElement("td");
      priceCell.textContent = new Intl.NumberFormat(undefined, { style: "currency", currency: product.currency }).format(price);
      const statusCell = document.createElement("td");
      const badge = document.createElement("span");
      badge.className = `badge ${product.status.toLowerCase() === "active" ? "active" : "draft"}`;
      badge.textContent = product.status;
      statusCell.append(badge);
      const actionsCell = document.createElement("td");
      actionsCell.textContent = "Added";
      row.append(nameCell, categoryCell, priceCell, statusCell, actionsCell);
      document.querySelector(".page-products table tbody")?.prepend(row);
      updateRows();
      closeProductDialog();
      toast("Product added successfully.");
      return;
    }
    if (!event.target.matches("[data-demo-form]")) return;
    event.preventDefault();
    const feedback = event.target.querySelector("[data-feedback]");
    const intent = event.submitter?.dataset.intent;
    if (feedback) feedback.textContent = intent === "publish"
      ? "Publish preview selected. No information was sent or saved."
      : intent === "draft"
        ? "Draft preview selected. No information was sent or saved."
        : "Saved for this demo. No information was sent.";
    else toast("Saved for this demo. No information was sent.");
  });
  productDialog?.addEventListener("cancel", () => {
    productForm?.reset();
    clearProductErrors();
  });
  productForm?.addEventListener("input", (event) => {
    const key = event.target.name;
    if (!key) return;
    const message = productForm.querySelector(`[data-error-for="${key}"]`);
    if (message) { message.textContent = ""; message.classList.remove("field-error"); }
    event.target.removeAttribute("aria-invalid");
    const summary = productForm.querySelector("[data-product-error]");
    if (summary && !productForm.querySelector("[aria-invalid='true']")) summary.hidden = true;
  });
})();
