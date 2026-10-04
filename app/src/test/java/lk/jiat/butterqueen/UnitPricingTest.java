package lk.jiat.butterqueen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import lk.jiat.butterqueen.model.UnitPricing;

public class UnitPricingTest {
    @Test
    public void cakesUseHalfKilogramsAndPricePerKilogram() {
        assertEquals("kg", UnitPricing.normalize("", "Cakes"));
        assertEquals(0.5, UnitPricing.step("kg"), 0.001);
        assertEquals(10500.00, UnitPricing.itemTotal(4200.00, 2.5), 0.001);
        assertEquals(0.5, UnitPricing.normalizeQuantity(0, "kg"), 0.001);
    }

    @Test
    public void browniesAndCupcakesUseWholePieces() {
        assertEquals("pcs", UnitPricing.normalize("", "Brownies"));
        assertEquals("pcs", UnitPricing.normalize("piece", "Cupcakes"));
        assertEquals(3.0, UnitPricing.normalizeQuantity(2.6, "pcs"), 0.001);
        assertEquals(1050.00, UnitPricing.itemTotal(350.00, 3), 0.001);
    }

    @Test
    public void moneyRoundsToTwoDecimalsAndLegacyPacksNeedReview() {
        assertEquals(50.01, UnitPricing.itemTotal(100.01, 0.5), 0.001);
        assertFalse(UnitPricing.isSupported("box"));
        assertFalse(UnitPricing.isSupported("pack"));
        assertTrue(UnitPricing.isSupported("kg"));
    }
}
