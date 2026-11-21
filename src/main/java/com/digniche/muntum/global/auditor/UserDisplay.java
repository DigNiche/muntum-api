package com.digniche.muntum.global.auditor;

import java.util.UUID;

/**
 * 작성자 칸 하나를 화면에 보여 줄 값
 * - id: 실제 회원이면 회원 id, 탈퇴자·시스템·알 수 없음이면 null
 * - nickname: 회원 닉네임 또는 문구표의 문구
 */
public record UserDisplay(UUID id, String nickname) {
}
