package com.fengxue.menu.preference;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    public Result<PreferenceDtos.PreferencesDto> get() {
        return Result.success(preferenceService.get(CurrentUser.require()));
    }

    @PutMapping
    public Result<PreferenceDtos.PreferencesDto> update(
            @Valid @RequestBody PreferenceDtos.PreferencesDto request) {
        return Result.success(preferenceService.update(CurrentUser.require(), request));
    }
}
