package com.fengxue.menu.menu;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/today")
    public Result<MenuPlan> today(@RequestParam(required = false) String date,
                                  @RequestParam(defaultValue = "false") boolean regenerate) {
        return Result.success(menuService.today(CurrentUser.require(), date, regenerate));
    }

    @PostMapping("/today/reroll")
    public Result<MenuPlan> reroll(@Valid @RequestBody MenuDtos.RerollRequest request) {
        return Result.success(menuService.reroll(CurrentUser.require(), request));
    }

    @PostMapping("/today/save")
    public Result<Void> save(@Valid @RequestBody MenuDtos.SaveRequest request) {
        menuService.save(CurrentUser.require(), request);
        return Result.success();
    }
}
