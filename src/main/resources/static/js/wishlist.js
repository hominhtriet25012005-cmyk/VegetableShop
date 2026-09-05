(function () {
    "use strict";

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    async function request(url, method) {
        const headers = {"X-Requested-With": "XMLHttpRequest"};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        const response = await fetch(url, {method, headers});
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message || "Không thể cập nhật danh sách yêu thích");
        }
        return payload;
    }

    function updateCounts(payload) {
        document.querySelectorAll("[data-wishlist-count]").forEach(element => {
            element.textContent = payload.wishlistCount;
        });
        if (payload.cartTotalQuantity >= 0) {
            document.querySelectorAll("[data-cart-count]").forEach(element => {
                element.textContent = payload.cartTotalQuantity;
            });
        }
    }

    function renderToggle(button, wishlisted) {
        button.dataset.wishlisted = String(wishlisted);
        const actionLabel = wishlisted ? "Bỏ yêu thích" : "Thêm vào yêu thích";
        button.setAttribute("aria-pressed", String(wishlisted));
        button.setAttribute("aria-label", actionLabel);
        button.setAttribute("title", actionLabel);
        const icon = button.querySelector("i");
        const label = button.querySelector("[data-wishlist-label]");
        icon?.classList.toggle("fas", wishlisted);
        icon?.classList.toggle("far", !wishlisted);
        icon?.classList.toggle("text-danger", wishlisted);
        if (label) {
            label.textContent = actionLabel;
        }
    }

    function renderAllToggles(productId, wishlisted) {
        document.querySelectorAll(`[data-wishlist-toggle][data-product-id="${productId}"]`)
            .forEach(button => renderToggle(button, wishlisted));
    }

    function notify(icon, title) {
        if (window.Swal) {
            Swal.fire({icon, title, timer: 1600, showConfirmButton: false});
        }
    }

    document.addEventListener("click", async event => {
        const toggle = event.target.closest("[data-wishlist-toggle]");
        const move = event.target.closest("[data-wishlist-move]");
        if (!toggle && !move) {
            return;
        }
        event.preventDefault();
        const button = toggle || move;
        if (button.disabled) return;
        const productId = button.dataset.productId;
        button.disabled = true;
        try {
            let payload;
            if (move) {
                payload = await request(`/api/wishlist/items/${productId}/move-to-cart`, "POST");
                document.querySelector(`[data-wishlist-item][data-product-id="${productId}"]`)?.remove();
                document.querySelector("[data-wishlist-empty]")?.classList.toggle("d-none", payload.wishlistCount !== 0);
            } else {
                const isWishlisted = button.dataset.wishlisted === "true";
                payload = await request(
                    isWishlisted ? `/api/wishlist/items/${productId}` : "/api/wishlist/items?productId=" + productId,
                    isWishlisted ? "DELETE" : "POST"
                );
                renderAllToggles(productId, payload.wishlisted);
            }
            updateCounts(payload);
            notify("success", payload.message);
        } catch (error) {
            notify("error", error.message);
        } finally {
            button.disabled = false;
        }
    });
})();
