(function () {
    "use strict";

    if (document.querySelector("[data-chatbot-root]")) return;

    const stylesheet = document.createElement("link");
    stylesheet.rel = "stylesheet";
    stylesheet.href = "/css/chatbot.css";
    document.head.appendChild(stylesheet);

    const root = document.createElement("section");
    root.className = "shop-chatbot";
    root.dataset.chatbotRoot = "";
    root.innerHTML = `
        <button class="shop-chatbot-toggle" type="button" aria-label="Mở trợ lý mua hàng"
                aria-expanded="false" aria-controls="shop-chatbot-panel">
            <i class="fas fa-comments" aria-hidden="true"></i>
            <span>Hỗ trợ</span>
        </button>
        <div class="shop-chatbot-panel" id="shop-chatbot-panel" hidden>
            <header class="shop-chatbot-header">
                <div>
                    <strong>Trợ lý Vegetable Shop</strong>
                    <small data-chatbot-mode>Chế độ cơ bản · dữ liệu cửa hàng</small>
                </div>
                <button type="button" data-chatbot-close aria-label="Đóng chatbot">×</button>
            </header>
            <div class="shop-chatbot-messages" data-chatbot-messages role="log"
                 aria-live="polite" aria-relevant="additions"></div>
            <div class="shop-chatbot-suggestions" data-chatbot-suggestions></div>
            <form class="shop-chatbot-form" data-chatbot-form>
                <label class="visually-hidden" for="shop-chatbot-input">Nhập câu hỏi</label>
                <input id="shop-chatbot-input" data-chatbot-input maxlength="300" autocomplete="off"
                       placeholder="Ví dụ: Có nấm dưới 100.000đ không?" required>
                <button type="submit" aria-label="Gửi câu hỏi"><i class="fas fa-paper-plane"></i></button>
            </form>
            <p class="shop-chatbot-note">Không nhập mật khẩu hoặc thông tin thanh toán.</p>
        </div>`;
    document.body.appendChild(root);

    const toggle = root.querySelector(".shop-chatbot-toggle");
    const panel = root.querySelector(".shop-chatbot-panel");
    const closeButton = root.querySelector("[data-chatbot-close]");
    const messages = root.querySelector("[data-chatbot-messages]");
    const suggestions = root.querySelector("[data-chatbot-suggestions]");
    const form = root.querySelector("[data-chatbot-form]");
    const input = root.querySelector("[data-chatbot-input]");
    const submitButton = form.querySelector("button[type='submit']");
    const modeLabel = root.querySelector("[data-chatbot-mode]");

    function setOpen(open) {
        panel.hidden = !open;
        toggle.setAttribute("aria-expanded", String(open));
        if (open) input.focus();
    }

    function addMessage(text, sender, loading) {
        const item = document.createElement("div");
        item.className = `shop-chatbot-message shop-chatbot-message-${sender}`;
        if (loading) item.dataset.chatbotLoading = "";
        item.textContent = text;
        messages.appendChild(item);
        messages.scrollTop = messages.scrollHeight;
        return item;
    }

    function formatPrice(value) {
        return new Intl.NumberFormat("vi-VN", {style: "currency", currency: "VND", maximumFractionDigits: 0})
            .format(Number(value || 0));
    }

    function addProducts(products) {
        if (!Array.isArray(products)) return;
        products.forEach(product => {
            const card = document.createElement("a");
            const safeUrl = typeof product.detailUrl === "string" && /^\/product\/\d+$/.test(product.detailUrl)
                ? product.detailUrl
                : "/shop";
            card.className = "shop-chatbot-product";
            card.href = safeUrl;

            const image = document.createElement("img");
            image.src = typeof product.image === "string" && product.image ? product.image : "/img/hero-img.jpg";
            image.alt = "";
            image.loading = "lazy";

            const content = document.createElement("span");
            const name = document.createElement("strong");
            name.textContent = product.name || "Sản phẩm";
            const meta = document.createElement("small");
            meta.textContent = `${formatPrice(product.price)} / ${product.unit || "sản phẩm"} · Còn ${product.availableQuantity || 0}`;
            content.append(name, meta);
            card.append(image, content);
            messages.appendChild(card);
        });
        messages.scrollTop = messages.scrollHeight;
    }

    const actionRoutes = new Map([
        ["Mở Đơn hàng của tôi", "/my-orders"],
        ["Mở trang Liên hệ", "/contact"],
        ["Liên hệ cửa hàng", "/contact"],
        ["Đăng nhập", "/login"],
        ["Đăng ký tài khoản", "/register"],
        ["Quên mật khẩu", "/forgot-password"]
    ]);

    function renderSuggestions(items) {
        suggestions.replaceChildren();
        (Array.isArray(items) ? items : []).forEach(label => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = label;
            button.addEventListener("click", () => {
                const route = actionRoutes.get(label);
                if (route) {
                    window.location.href = route;
                    return;
                }
                input.value = label;
                form.requestSubmit();
            });
            suggestions.appendChild(button);
        });
    }

    async function ask(question) {
        addMessage(question, "user");
        renderSuggestions([]);
        input.disabled = true;
        submitButton.disabled = true;
        const loading = addMessage("Đang tìm thông tin...", "bot", true);
        try {
            const response = await fetch(`/api/chatbot/messages?message=${encodeURIComponent(question)}`, {
                method: "GET",
                headers: {"Accept": "application/json"},
                credentials: "same-origin",
                cache: "no-store"
            });
            const data = await response.json();
            loading.remove();
            modeLabel.textContent = data.mode === "AI"
                ? "AI · kiểm chứng bằng dữ liệu cửa hàng"
                : data.mode === "FAQ"
                    ? "FAQ chính thức của cửa hàng"
                    : "Chế độ cơ bản · dữ liệu cửa hàng";
            addMessage(data.message || "Chatbot chưa thể trả lời lúc này.", "bot");
            addProducts(data.products);
            renderSuggestions(data.suggestions);
        } catch (error) {
            loading.remove();
            modeLabel.textContent = "Chế độ cơ bản · tạm mất kết nối";
            addMessage("Không thể kết nối chatbot. Bạn vui lòng thử lại sau.", "bot");
            renderSuggestions(["Tìm sản phẩm", "Liên hệ cửa hàng"]);
        } finally {
            input.disabled = false;
            submitButton.disabled = false;
            input.focus();
        }
    }

    toggle.addEventListener("click", () => setOpen(panel.hidden));
    closeButton.addEventListener("click", () => setOpen(false));
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !panel.hidden) setOpen(false);
    });
    form.addEventListener("submit", event => {
        event.preventDefault();
        const question = input.value.trim();
        if (!question || question.length > 300) return;
        input.value = "";
        ask(question);
    });

    addMessage("Xin chào! Mình có thể giúp bạn tìm sản phẩm và giải đáp các câu hỏi mua hàng.", "bot");
    renderSuggestions(["Tìm nấm dưới 100.000đ", "Phí giao hàng", "Thanh toán thế nào?", "Xem đơn hàng của tôi"]);
})();
