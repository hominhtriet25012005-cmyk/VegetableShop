package com.vegetableshop;

import com.vegetableshop.controller.HomeController;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.service.CategoryService;
import com.vegetableshop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VegetableShopApplicationTests {

	@Test
	void homeControllerReturnsIndexTemplate() {
		HomeController controller = new HomeController(Optional.empty(), Optional.empty(), Optional.empty());
		assertEquals("index", controller.home(null, new ConcurrentModel()));
	}

	@Test
	void homeControllerAddsProductsAndCategoriesToModel() {
		ProductService productService = mock(ProductService.class);
		CategoryService categoryService = mock(CategoryService.class);
		Product product = new Product();
		Category category = new Category();
		when(productService.findHomepageCatalog(List.of(category))).thenReturn(List.of(product));
		when(categoryService.findAllActiveCategories()).thenReturn(List.of(category));

		ConcurrentModel model = new ConcurrentModel();
		HomeController controller = new HomeController(
			Optional.of(productService), Optional.of(categoryService), Optional.empty());

		assertEquals("index", controller.home(null, model));
		assertEquals(List.of(product), model.getAttribute("homeProducts"));
		assertEquals(1, model.getAttribute("homeFeaturedCount"));
		assertEquals(List.of(category), model.getAttribute("categories"));
	}

}
