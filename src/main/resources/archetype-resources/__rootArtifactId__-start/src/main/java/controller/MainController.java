#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ${package}.auth.user.SysUserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Controller
@SuppressWarnings("unused")
public class MainController {
    private static final List<String> FAVICON_STYLES = Arrays.asList("dark", "light");

    @Value("${symbol_dollar}{favicon.style:light}")
    private String faviconStyle;

    @RequestMapping("/")
    public String index(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication)) {
            if (authentication.getPrincipal() instanceof SysUserDetails sysUser) {
                String name = sysUser.getNickname();
                if (StringUtils.isBlank(name)) {
                    name = sysUser.getUsername();
                }
                model.addAttribute("name", name);
            } else {
                model.addAttribute("name", authentication.getName());
            }
        }
        return "hello";
    }

    @GetMapping("/public/check/live")
    @ResponseBody
    public String live() {
        return "OK";
    }

    @GetMapping("/public/check/ready")
    @ResponseBody
    public String ready() {
        // TODO: check services statuses
        return "OK";
    }

    @GetMapping("/favicon.ico")
    public String favicon() {
        String style = faviconStyle;
        if (!FAVICON_STYLES.contains(style)) {
            style = FAVICON_STYLES.get(new Random().nextInt(FAVICON_STYLES.size()));
        }
        return String.format("forward:/%s/favicon.ico", style);
    }
}
