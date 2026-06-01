package com.iwas.skill.dto;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.skill.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One required-skill constraint: the subject must own {@code skillId} at {@code minLevel}
 * or higher. A {@code null} {@code minLevel} means "owns the skill, any level". Shared by
 * global user search and project-scoped member search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredSkill {

    private Long skillId;
    private SkillLevel minLevel;

    /**
     * Parses the compact {@code requiredSkills} query param,
     * e.g. {@code "12:ADVANCED,5:INTERMEDIATE,7"}. Each item is
     * {@code skillId[:minLevel]}; the level part is optional. Duplicate skill ids
     * keep the last occurrence. Returns an empty list for {@code null}/blank input.
     *
     * @throws AppException SEARCH_INVALID_SKILL_FILTER on malformed items
     */
    public static List<RequiredSkill> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Map<Long, RequiredSkill> bySkillId = new LinkedHashMap<>();
        for (String token : raw.split(",")) {
            String item = token.trim();
            if (item.isEmpty()) {
                continue;
            }
            String[] parts = item.split(":", 2);
            Long skillId;
            try {
                skillId = Long.valueOf(parts[0].trim());
            } catch (NumberFormatException e) {
                throw new AppException(ErrorCode.SEARCH_INVALID_SKILL_FILTER,
                        "Invalid skillId in requiredSkills: '" + item + "'");
            }
            SkillLevel minLevel = null;
            if (parts.length == 2 && !parts[1].isBlank()) {
                try {
                    minLevel = SkillLevel.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new AppException(ErrorCode.SEARCH_INVALID_SKILL_FILTER,
                            "Invalid skill level in requiredSkills: '" + item + "'");
                }
            }
            bySkillId.put(skillId, RequiredSkill.builder().skillId(skillId).minLevel(minLevel).build());
        }
        return new ArrayList<>(bySkillId.values());
    }

    /**
     * The set of skill levels that satisfy this constraint (levels {@code >= minLevel}).
     * A {@code null} {@code minLevel} accepts every level.
     */
    public List<SkillLevel> acceptedLevels() {
        List<SkillLevel> levels = new ArrayList<>();
        for (SkillLevel level : SkillLevel.values()) {
            if (minLevel == null || level.ordinal() >= minLevel.ordinal()) {
                levels.add(level);
            }
        }
        return levels;
    }

    /** Minimum acceptable level rank (ordinal); 0 when no level constraint. */
    public int minLevelRank() {
        return minLevel == null ? 0 : minLevel.ordinal();
    }
}
