package group7.enrollmentSystem.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @GetMapping
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = (status != null) ? Integer.parseInt(status.toString()) : 500;

        // Extract the exception, if available
        Exception exception = (Exception) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String exceptionMessage = (exception != null) ? exception.getMessage() : "No additional details available.";

        StringBuilder errorMessage = new StringBuilder();
        switch (statusCode) {
            case 404 -> errorMessage.append("The page you are looking for does not exist.");
            case 403 -> errorMessage.append("You do not have permission to access this page.");
            case 500 -> errorMessage.append("An internal server error occurred. Please try again later.");
            default -> errorMessage.append("An unexpected error occurred.");
        }

        if (statusCode == 500 && exception != null) {
            model.addAttribute("errorDetails", exceptionMessage);
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", errorMessage.toString());

        return "error-page";
    }
}
