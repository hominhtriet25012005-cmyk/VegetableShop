(() => {
    "use strict";

    document.addEventListener("click", event => {
        const thumbnail = event.target.closest("[data-gallery-thumb]");
        if (thumbnail) {
            const gallery = thumbnail.closest("[data-product-gallery]");
            const mainImage = gallery?.querySelector("[data-gallery-main]");
            if (!mainImage || !thumbnail.dataset.imageUrl) return;
            mainImage.src = thumbnail.dataset.imageUrl;
            gallery.querySelectorAll("[data-gallery-thumb]").forEach(button => {
                const selected = button === thumbnail;
                button.classList.toggle("active", selected);
                button.setAttribute("aria-pressed", String(selected));
            });
            return;
        }

        const quantityButton = event.target.closest("[data-detail-minus], [data-detail-plus]");
        if (!quantityButton) return;
        const form = quantityButton.closest("form");
        const input = form?.querySelector("[data-detail-quantity]");
        if (!input) return;
        const minimum = Number(input.min || 1);
        const maximum = Number(input.max || Number.MAX_SAFE_INTEGER);
        const change = quantityButton.hasAttribute("data-detail-plus") ? 1 : -1;
        input.value = String(Math.min(maximum, Math.max(minimum, Number(input.value || minimum) + change)));
    });
})();
