(() => {
  const destinations = {
    halong: {
      title: "Ha Long Bay Weekend Escape",
      product: "Ha Long Bay cruise",
      location: "Ha Long Bay, Vietnam",
      shortLocation: "Ha Long Bay",
      price: 420,
      image: "../assets/images/halong-bay.svg",
      description: "A slow sailing journey through emerald waters and limestone karsts.",
      duration: "Two unhurried nights",
      feeling: "Quiet discovery",
      number: "01",
    },
    hoian: {
      title: "Hoi An Lantern Season",
      product: "Hoi An riverside stay",
      location: "Hoi An, Vietnam",
      shortLocation: "Hoi An",
      price: 280,
      image: "../assets/images/hoi-an.svg",
      description: "Stay by the river and discover lantern-lit streets after dusk.",
      duration: "Three nights",
      feeling: "Old town evenings",
      number: "02",
    },
    phuquoc: {
      title: "Phu Quoc Island Escape",
      product: "Phu Quoc beach stay",
      location: "Phu Quoc, Vietnam",
      shortLocation: "Phu Quoc",
      price: 380,
      image: "../assets/images/phu-quoc.svg",
      description: "A sunlit island hideaway with soft sand and clear blue water.",
      duration: "Four island nights",
      feeling: "Coastal reset",
      number: "03",
    },
  };
  const featuredJourneys = [
    { key: "halong", region: "THE NORTHEAST COAST", eyebrow: "THE SIGNATURE ESCAPE", title: "Ha Long Bay, unhurried", duration: "Two nights on the water" },
    { key: "hoian", region: "THE OLD TOWN", eyebrow: "A SLOWER CITY STAY", title: "Hoi An after the lanterns", duration: "Three nights by the river" },
    { key: "phuquoc", region: "THE SOUTHERN ISLANDS", eyebrow: "THE ISLAND EDIT", title: "Phu Quoc, all to yourself", duration: "Four nights by the sea" },
  ];
  const heroSlider = document.querySelector("[data-hero-slider]");
  if (heroSlider) {
    const heroImage = heroSlider.querySelector("[data-hero-image]");
    const heroSlides = featuredJourneys.map((journey) => ({ ...journey, ...destinations[journey.key] }));
    let activeHero = 0;
    const showHeroSlide = (index) => {
      activeHero = (index + heroSlides.length) % heroSlides.length;
      const slide = heroSlides[activeHero];
      heroImage.classList.add("is-changing");
      window.setTimeout(() => {
        heroImage.src = slide.image;
        heroImage.alt = `${slide.shortLocation}, Vietnam`;
        heroSlider.querySelector("[data-hero-region]").textContent = slide.region;
        heroSlider.querySelector("[data-hero-count]").innerHTML = `${String(activeHero + 1).padStart(2, "0")} <i>/</i> 03`;
        heroSlider.querySelector("[data-hero-eyebrow]").textContent = slide.eyebrow;
        heroSlider.querySelector("[data-hero-title]").textContent = slide.title;
        heroSlider.querySelector("[data-hero-duration]").textContent = slide.duration;
        heroSlider.querySelector("[data-hero-price]").textContent = formatPrice(slide.price);
        const link = heroSlider.querySelector("[data-hero-link]");
        link.href = `product-detail.html?product=${slide.key}`;
        link.setAttribute("aria-label", `Discover ${slide.title}`);
        heroSlider.querySelectorAll("[data-hero-dot]").forEach((dot, dotIndex) => {
          const selected = dotIndex === activeHero;
          dot.classList.toggle("is-active", selected);
          if (selected) dot.setAttribute("aria-current", "true");
          else dot.removeAttribute("aria-current");
        });
        heroImage.classList.remove("is-changing");
      }, 140);
    };
    heroSlider.querySelector("[data-hero-prev]").addEventListener("click", () => showHeroSlide(activeHero - 1));
    heroSlider.querySelector("[data-hero-next]").addEventListener("click", () => showHeroSlide(activeHero + 1));
    heroSlider.querySelectorAll("[data-hero-dot]").forEach((dot) => {
      dot.addEventListener("click", () => showHeroSlide(Number(dot.dataset.heroDot)));
    });
  }
  const queryProduct = new URLSearchParams(location.search).get("product");
  let rememberedProduct = "halong";
  try { rememberedProduct = sessionStorage.getItem("storefront-product") || "halong"; } catch { /* Private browsing may disable storage. */ }
  const key = queryProduct || rememberedProduct;
  const item = destinations[key] || destinations.halong;
  try { sessionStorage.setItem("storefront-product", key in destinations ? key : "halong"); } catch { /* The URL still carries the chosen product. */ }

  const setText = (selector, value) => {
    const node = document.querySelector(selector);
    if (node) node.textContent = value;
  };
  const setImage = (selector, source, alt) => {
    const node = document.querySelector(selector);
    if (node) {
      node.src = source;
      if (alt !== undefined) node.alt = alt;
    }
  };
  const formatPrice = (value) => `$${new Intl.NumberFormat("en-US").format(value)}`;
  const productKey = destinations[key] ? key : "halong";

  if (document.querySelector("[data-product-detail]")) {
    setText("[data-detail-title]", item.title);
    setText("[data-detail-product]", item.product);
    setText("[data-detail-location]", item.location.toUpperCase());
    setText("[data-detail-breadcrumb]", item.shortLocation);
    setText("[data-detail-number]", item.number);
    setText("[data-detail-duration]", item.duration);
    setText("[data-detail-style]", item.feeling);
    setText("[data-detail-description]", item.description);
    setText("[data-detail-price]", `${formatPrice(item.price)} / guest`);
    setImage("[data-detail-image]", item.image, `${item.shortLocation}, Vietnam`);
    document.querySelectorAll("[data-book-link], [data-book-link-bottom]").forEach((link) => {
      link.href = `checkout.html?product=${productKey}`;
    });
  }

  const bookingForm = document.querySelector("[data-booking-form]");
  if (bookingForm) {
    setText("[data-summary-title]", item.title);
    setText("[data-summary-location]", item.location);
    setText("[data-summary-price]", formatPrice(item.price));
    setImage("[data-summary-image]", item.image, `${item.shortLocation} landscape`);
    const backLink = document.querySelector("[data-back-product]");
    if (backLink) backLink.href = `product-detail.html?product=${productKey}`;

    const guests = bookingForm.elements.guests;
    const refreshSummary = () => {
      const count = Number.parseInt(guests.value, 10) || 1;
      setText("[data-summary-guests]", guests.value);
      setText("[data-summary-total]", formatPrice(item.price * count));
    };
    guests.addEventListener("change", refreshSummary);
    refreshSummary();

    bookingForm.addEventListener("submit", (event) => {
      event.preventDefault();
      if (!bookingForm.reportValidity()) return;
      const values = new FormData(bookingForm);
      setText("[data-confirmation-name]", values.get("firstName"));
      setText("[data-confirmation-product]", item.title);
      document.querySelector("[data-confirmation]").hidden = false;
      bookingForm.closest(".checkout-grid").hidden = true;
      document.querySelector(".checkout-head").hidden = true;
      document.querySelector("[data-confirmation]").scrollIntoView({ behavior: "smooth", block: "center" });
    });
  }

  document.querySelector("[data-trip-search]")?.addEventListener("input", (event) => {
    const term = event.target.value.trim().toLowerCase();
    document.querySelectorAll(".trip-card").forEach((card) => {
      card.hidden = !card.textContent.toLowerCase().includes(term);
    });
  });
})();
