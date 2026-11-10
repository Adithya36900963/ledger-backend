package snovasys.ledger.admin.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.admin.model.AdminUserView;
import snovasys.ledger.admin.service.AdminUserService;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminController {
    private final AdminUserService adminUserService;

    @GetMapping("/users")
    public List<AdminUserView> users() { return adminUserService.listUsers(); }
}
