package com.fengxue.menu.preference;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import com.fengxue.menu.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final UserPreferenceMapper preferenceMapper;

    public PreferenceDtos.PreferencesDto get(Long userId) {
        UserPreference entity = preferenceMapper.selectById(userId);
        return entity == null ? defaults() : toDto(entity);
    }

    public PreferenceDtos.PreferencesDto update(Long userId, PreferenceDtos.PreferencesDto dto) {
        validate(dto);
        UserPreference entity = toEntity(userId, dto);
        if (preferenceMapper.selectById(userId) == null) {
            preferenceMapper.insert(entity);
        } else {
            preferenceMapper.updateById(entity);
        }
        return dto;
    }

    public void createDefault(Long userId) {
        if (preferenceMapper.selectById(userId) != null) {
            return;
        }
        preferenceMapper.insert(toEntity(userId, defaults()));
    }

    public PreferenceDtos.PreferencesDto defaults() {
        return new PreferenceDtos.PreferencesDto(
                2, 3, 3, 3, 4, 90, 2, List.of(), List.of());
    }

    private void validate(PreferenceDtos.PreferencesDto dto) {
        if (dto.meatRatio() + dto.vegRatio() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "荤素比例不能同时为 0");
        }
    }

    private UserPreference toEntity(Long userId, PreferenceDtos.PreferencesDto dto) {
        UserPreference entity = new UserPreference();
        entity.setUserId(userId);
        entity.setPeopleCount(dto.peopleCount());
        entity.setMealsPerDay(dto.mealsPerDay());
        entity.setDishesPerMeal(dto.dishesPerMeal());
        entity.setMeatRatio(dto.meatRatio());
        entity.setVegRatio(dto.vegRatio());
        entity.setMaxCookMinutes(dto.maxCookMinutes());
        entity.setSpiceLevel(dto.spiceLevel());
        entity.setAvoidIngredientsJson(JsonSupport.toJson(
                dto.avoidIngredients() == null ? List.of() : dto.avoidIngredients()));
        entity.setPreferredCategoriesJson(JsonSupport.toJson(
                dto.preferredCategories() == null ? List.of() : dto.preferredCategories()));
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private PreferenceDtos.PreferencesDto toDto(UserPreference entity) {
        return new PreferenceDtos.PreferencesDto(
                entity.getPeopleCount(),
                entity.getMealsPerDay(),
                entity.getDishesPerMeal(),
                entity.getMeatRatio(),
                entity.getVegRatio(),
                entity.getMaxCookMinutes(),
                entity.getSpiceLevel(),
                stringList(entity.getAvoidIngredientsJson()),
                longList(entity.getPreferredCategoriesJson()));
    }

    private List<String> stringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            String[] values = JsonSupport.fromJson(json, String[].class);
            return List.of(values);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Long> longList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Long[] values = JsonSupport.fromJson(json, Long[].class);
            return new ArrayList<>(List.of(values));
        } catch (Exception e) {
            return List.of();
        }
    }
}
