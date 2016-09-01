import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by eliapalme on 20/06/16.
 */
@Ignore("Test is ignored, used for manual testing")
public class TestPhantomJSPathsRetreival {


    @Test
    public void testPathsRetreival() throws Exception {

        String html;
        WebDriver driver = new RemoteWebDriver(new URL("http://46.4.71.105:31555"), DesiredCapabilities.phantomjs());
        try {

            driver.navigate().to("http://www.sonntagszeitung.ch/");
            Wait<WebDriver> wait = new WebDriverWait(driver, 30);
            wait.until(_driver -> String.valueOf(((JavascriptExecutor) _driver).executeScript("return document.readyState")).equals("complete"));
            html = driver.getPageSource();

            List<WebElement> elementList = driver.findElements(By.tagName("div"));
            Set<String> urls = new HashSet<>();
            for (WebElement element : elementList) {
                if (!element.isDisplayed() || !element.isEnabled()) {
                    continue;
                }
                element.click();
                wait.until(_driver -> String.valueOf(((JavascriptExecutor) _driver).executeScript("return document.readyState")).equals("complete"));
                urls.add(driver.getCurrentUrl());
                driver.navigate().back();
            }

            System.out.println("Size:" + urls.size());


        } finally {
            driver.quit();
        }


    }

}
