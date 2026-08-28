(() => {
    "use strict";

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
    const currency = new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    });

    const showMessage = (icon, message) => {
        if (window.Swal) {
            window.Swal.fire({
                toast: true,
                position: "top-end",
                icon,
                title: message,
                showConfirmButton: false,
                timer: 1800,
                timerProgressBar: true
            });
            return;
        }
        window.alert(message);
    };

    const apiRequest = async (url, options = {}) => {
        const headers = new Headers(options.headers || {});
        headers.set("Accept", "application/json");
        if (csrfToken) headers.set(csrfHeader, csrfToken);
        const response = await fetch(url, {...options, headers});
        const payload = await response.json().catch(() => ({message: "Phản hồi không hợp lệ từ máy chủ"}));
        if (!response.ok) throw new Error(payload.message || "Không thể cập nhật giỏ hàng");
        return payload;
    };

    const setBusy = (form, busy) => {
        form.querySelectorAll("button,input").forEach(element => element.disabled = busy);
        form.classList.toggle("cart-busy", busy);
    };

    const updateSummary = data => {
        document.querySelectorAll("[data-cart-count]").forEach(element => {
            element.textContent = data.totalQuantity;
            element.classList.remove("cart-badge-pulse");
            void element.offsetWidth;
            element.classList.add("cart-badge-pulse");
        });
        document.querySelectorAll("[data-cart-total-quantity]")
            .forEach(element => element.textContent = data.totalQuantity);
        document.querySelectorAll("[data-cart-total]")
            .forEach(element => element.textContent = currency.format(data.cartTotal));
    };

    const flyToCart = form => {
        const source = form.closest(".fruite-item, .card, .border")?.querySelector("img");
        const target = document.querySelector("[data-cart-icon]");
        if (!source || !target || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
        const from = source.getBoundingClientRect();
        const to = target.getBoundingClientRect();
        const clone = source.cloneNode(true);
        clone.className = "cart-fly-image";
        Object.assign(clone.style, {left: `${from.left}px`, top: `${from.top}px`});
        document.body.appendChild(clone);
        requestAnimationFrame(() => {
            clone.style.transform = `translate(${to.left - from.left}px, ${to.top - from.top}px) scale(.15)`;
            clone.style.opacity = "0.2";
        });
        clone.addEventListener("transitionend", () => clone.remove(), {once: true});
        window.setTimeout(() => clone.remove(), 900);
    };

    document.addEventListener("submit", async event => {
        const addForm = event.target.closest("form[data-cart-add]");
        if (addForm) {
            event.preventDefault();
            // FormData ignores disabled controls. Capture productId and quantity
            // before setBusy() disables the form while the request is running.
            const body = new URLSearchParams(new FormData(addForm));
            setBusy(addForm, true);
            try {
                const data = await apiRequest("/api/cart/items", {
                    method: "POST",
                    headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
                    body
                });
                updateSummary(data);
                flyToCart(addForm);
                showMessage("success", data.message);
            } catch (error) {
                showMessage("error", error.message);
            } finally {
                setBusy(addForm, false);
            }
            return;
        }

        const updateForm = event.target.closest("form[data-cart-update]");
        if (updateForm) {
            event.preventDefault();
            const input = updateForm.querySelector("[data-cart-quantity]");
            setBusy(updateForm, true);
            try {
                const body = new URLSearchParams({quantity: input.value});
                const data = await apiRequest(`/api/cart/items/${updateForm.dataset.itemId}`, {
                    method: "PATCH",
                    headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
                    body
                });
                input.value = data.quantity;
                input.max = data.stock;
                updateForm.closest("[data-cart-row]").querySelector("[data-cart-subtotal]").textContent =
                    currency.format(data.itemSubtotal);
                updateSummary(data);
                showMessage("success", data.message);
            } catch (error) {
                showMessage("error", error.message);
            } finally {
                setBusy(updateForm, false);
            }
        }
    });

    document.addEventListener("click", async event => {
        const quantityButton = event.target.closest("[data-cart-minus], [data-cart-plus]");
        if (quantityButton) {
            const form = quantityButton.closest("form[data-cart-update]");
            const input = form.querySelector("[data-cart-quantity]");
            const step = quantityButton.hasAttribute("data-cart-plus") ? 1 : -1;
            const next = Math.min(Number(input.max), Math.max(1, Number(input.value) + step));
            if (next !== Number(input.value)) {
                input.value = next;
                form.requestSubmit();
            }
            return;
        }

        const deleteButton = event.target.closest("form[data-cart-delete] button");
        if (!deleteButton) return;
        event.preventDefault();
        const form = deleteButton.closest("form[data-cart-delete]");
        let confirmed = true;
        if (window.Swal) {
            const result = await window.Swal.fire({
                icon: "warning",
                title: "Xóa khỏi giỏ hàng?",
                text: form.dataset.productName || "Sản phẩm đã chọn",
                showCancelButton: true,
                confirmButtonText: "Xóa",
                cancelButtonText: "Giữ lại",
                confirmButtonColor: "#dc3545"
            });
            confirmed = result.isConfirmed;
        } else {
            confirmed = window.confirm("Bạn có chắc muốn xóa sản phẩm này?");
        }
        if (!confirmed) return;

        setBusy(form, true);
        try {
            const data = await apiRequest(`/api/cart/items/${form.dataset.itemId}`, {method: "DELETE"});
            const row = form.closest("[data-cart-row]");
            row.classList.add("cart-row-removing");
            window.setTimeout(() => row.remove(), 300);
            updateSummary(data);
            if (data.empty) {
                window.setTimeout(() => {
                    document.querySelector("[data-cart-content]")?.classList.add("d-none");
                    document.querySelector("[data-cart-empty]")?.classList.remove("d-none");
                }, 300);
            }
            showMessage("success", data.message);
        } catch (error) {
            setBusy(form, false);
            showMessage("error", error.message);
        }
    });
})();
