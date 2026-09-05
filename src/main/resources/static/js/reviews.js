(function () {
    'use strict';
    const input = document.querySelector('[data-review-images]');
    const preview = document.querySelector('[data-review-preview]');
    if (!input || !preview) return;
    let urls = [];
    input.addEventListener('change', function () {
        urls.forEach(url => URL.revokeObjectURL(url));
        urls = [];
        preview.replaceChildren();
        input.setCustomValidity('');
        const files = Array.from(input.files || []);
        if (files.length > 5 || files.some(file => file.size > 2 * 1024 * 1024 || !['image/jpeg', 'image/png'].includes(file.type))) {
            input.setCustomValidity('Chọn tối đa 5 ảnh JPG/PNG, mỗi ảnh không quá 2 MB.');
            input.reportValidity();
            return;
        }
        files.forEach(file => {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(file);
            urls.push(img.src);
            img.alt = 'Ảnh sẽ gửi đánh giá';
            img.width = 80; img.height = 80;
            img.className = 'rounded border'; img.style.objectFit = 'cover';
            preview.append(img);
        });
    });
})();
