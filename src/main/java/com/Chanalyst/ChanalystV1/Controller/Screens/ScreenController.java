package com.Chanalyst.ChanalystV1.Controller.Screens;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ScreenController {

    @GetMapping("/game")
    public String gamePage() {
        // Spring will look for index.html in /static or /templates
        return "index.html";
    }

}
