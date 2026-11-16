
package com.digniche.muntum.user.service;

import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.UserTermsType;
import com.digniche.muntum.user.dto.response.TermsResponse;
import com.digniche.muntum.user.repository.TermsRepository;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

public class PublicTermsCatalogSmokeTest {

    public static void main(String[] args) {

        Terms requiredTerms = Terms.builder()
                .type(UserTermsType.TERMS_OF_SERVICE)
                .version("1.0")
                .title("Service Terms")
                .content("Example service terms")
                .effectiveAt(LocalDateTime.now())
                .build();

        requiredTerms.activate();

        Terms optionalTerms = Terms.builder()
                .type(UserTermsType.MARKETING_EMAIL)
                .version("2.0")
                .title("Email Marketing")
                .content("Example optional agreement")
                .effectiveAt(LocalDateTime.now())
                .build();

        optionalTerms.activate();

        // Simulate the repository without MySQL.
        TermsRepository fakeRepository =
                (TermsRepository) Proxy.newProxyInstance(
                        TermsRepository.class.getClassLoader(),
                        new Class<?>[]{TermsRepository.class},
                        (proxy, method, arguments) -> {
                            if (method.getName().equals(
                                    "findAllByActiveTrueAndDeletedAtIsNull"
                            )) {
                                return List.of(
                                        requiredTerms,
                                        optionalTerms
                                );
                            }

                            throw new UnsupportedOperationException(
                                    method.getName()
                            );
                        }
                );

        TermsService service =
                new TermsService(fakeRepository);

        List<TermsResponse> result =
                service.getPublicActiveTerms();

        check(result.size() == 2, "Expected two terms");

        check(
                result.get(0).type()
                        == UserTermsType.TERMS_OF_SERVICE,
                "Incorrect required terms type"
        );

        check(
                result.get(0).required(),
                "Service terms should be required"
        );

        check(
                !result.get(1).required(),
                "Marketing consent should be optional"
        );

        check(
                result.get(1).version().equals("2.0"),
                "Incorrect version"
        );

        check(
                result.get(1).content()
                        .equals("Example optional agreement"),
                "Content was not mapped"
        );

        System.out.println(
                "Public terms catalog smoke test passed."
        );
    }

    private static void check(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
