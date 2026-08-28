(() => {
    "use strict";

    const filters = Array.from(document.querySelectorAll("[data-home-category-filter]"));
    const products = Array.from(document.querySelectorAll("[data-home-product-wrapper]"));
    const emptyMessage = document.querySelector("[data-home-filter-empty]");
    const visibleLimit = 8;

    if (filters.length === 0 || products.length === 0) {
        return;
    }

    const applyFilter = (categoryId) => {
        let visibleCount = 0;

        products.forEach((product) => {
            const matches = categoryId === "all" || product.dataset.categoryId === categoryId;
            const shouldShow = matches && visibleCount < visibleLimit;
            product.classList.toggle("d-none", !shouldShow);
            if (shouldShow) {
                visibleCount += 1;
            }
        });

        filters.forEach((filter) => {
            const active = filter.dataset.categoryId === categoryId;
            filter.classList.toggle("btn-primary", active);
            filter.classList.toggle("bg-light", !active);
            filter.setAttribute("aria-pressed", String(active));
        });

        if (emptyMessage) {
            emptyMessage.classList.toggle("d-none", visibleCount > 0);
        }
    };

    filters.forEach((filter) => {
        filter.addEventListener("click", () => applyFilter(filter.dataset.categoryId));
    });

    applyFilter("all");
})();
