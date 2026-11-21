package com.digniche.muntum.global.auditor;

import java.util.Objects;

/**
 * 특수 UUID User의 화면에 나타날 닉네임(문구)
 * - 서비스마다 자기 문구표를 하나씩 둔다 (문구는 화면마다 다를 수 있음)
 * - 네 문구를 모두 채워야 만들 수 있어, 한 종류를 빠뜨릴 수 없다
 */
public record UserDisplayLabels(
        String withdrawnManager,
        String withdrawnCurator,
        String withdrawnAudience,
        String system
) {

    public UserDisplayLabels {
        Objects.requireNonNull(withdrawnManager, "withdrawnManager");
        Objects.requireNonNull(withdrawnCurator, "withdrawnCurator");
        Objects.requireNonNull(withdrawnAudience, "withdrawnAudience");
        Objects.requireNonNull(system, "system");
    }

    public String labelOf(SpecialUserType type) {
        return switch (type) {
            case WITHDRAWN_MANAGER -> withdrawnManager;
            case WITHDRAWN_CURATOR -> withdrawnCurator;
            case WITHDRAWN_AUDIENCE -> withdrawnAudience;
            case SYSTEM -> system;
        };
    }
}
