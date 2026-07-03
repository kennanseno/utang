package com.utang.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void normalizesLocalPhilippineMobile() {
        assertThat(PhoneNumbers.toE164("09171234567")).isEqualTo("+639171234567");
        assertThat(PhoneNumbers.toE164("0917 123 4567")).isEqualTo("+639171234567");
        assertThat(PhoneNumbers.toE164("0917-123-4567")).isEqualTo("+639171234567");
    }

    @Test
    void normalizesBareNationalNumber() {
        assertThat(PhoneNumbers.toE164("9171234567")).isEqualTo("+639171234567");
    }

    @Test
    void normalizesCountryCodeVariants() {
        assertThat(PhoneNumbers.toE164("639171234567")).isEqualTo("+639171234567");
        assertThat(PhoneNumbers.toE164("+639171234567")).isEqualTo("+639171234567");
        assertThat(PhoneNumbers.toE164("00639171234567")).isEqualTo("+639171234567");
    }

    @Test
    void preservesOtherCountryNumbers() {
        assertThat(PhoneNumbers.toE164("+353831015611")).isEqualTo("+353831015611");
    }

    @Test
    void rejectsBlankOrInvalid() {
        assertThatThrownBy(() -> PhoneNumbers.toE164(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneNumbers.toE164("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneNumbers.toE164("12345"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneNumbers.toE164("not-a-number"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
