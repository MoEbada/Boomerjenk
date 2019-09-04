package testlink;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.pramcharan.wd.binary.downloader.WebDriverBinaryDownloader;
import io.github.pramcharan.wd.binary.downloader.enums.Browser;

public class AssemblyDownloader {

	WebDriver driver = null;

	private AssemblyDownloader() {
		/**
		 * This constructor should not be used. Use AssemblyDownloader(String
		 * assemblyPathOnServer) instead to set assemblies path on server
		 */
	}

	/**
	 * Initiates selenium in order to download assemblies
	 * 
	 * @param assemblyPathOnServer link of Jenkins artifacts containing assemblies
	 *                             (last successful build)
	 */
	public AssemblyDownloader(String assemblyPathOnServer) {
		initSelenium();
		this.driver = createFirefoxDriverWithDownloadFolder(Config.getInstance().getAssembliesPathOnDisk());
		Config.getInstance().assembliesPathOnServer = assemblyPathOnServer;
	}

	private void initSelenium() {
		WebDriverBinaryDownloader.create().downloadLatestBinaryAndConfigure(Browser.FIREFOX);
	}

	/**
	 * Creates Selenium firefox driver and set download directory
	 * 
	 * @param downloadFolder where assemblies will be downloaded
	 * @return
	 */
	private WebDriver createFirefoxDriverWithDownloadFolder(String downloadFolder) {
		FirefoxProfile profile = new FirefoxProfile();
		FirefoxOptions options = new FirefoxOptions();
		profile.setPreference("browser.download.folderList", 2);
		profile.setPreference("browser.download.dir", downloadFolder);
		profile.setPreference("browser.download.manager.showWhenStarting", false);
		profile.setPreference("browser.helperApps.neverAsk.openFile", "application/x-msdownload");
		profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/x-msdownload, application/zip");

		options.setProfile(profile);

		WebDriver driver = new FirefoxDriver(options);
		return driver;
	}

	/**
	 * Downloads *.Test.dll files only
	 */
	void downloadAssembliesOnly() {
		this.driver.navigate().to(Config.getInstance().assembliesPathOnServer);
		WebElement fileList = new WebDriverWait(driver, Duration.ofSeconds(10))
				.until(ExpectedConditions.presenceOfElementLocated(By.className("fileList")));

		for (WebElement file : fileList.findElements(By.cssSelector("a[href$='Test.dll']"))) {
			file.click();
			System.out.println(file.getAttribute("href"));
		}
		this.driver.close();
		this.driver.quit();
	}

	/**
	 * Downloads all assemblies and their dependencies as a zip file
	 */
	void downloadAssembliesAndUnzipArchive() {
		this.driver.navigate().to(Config.getInstance().assembliesPathOnServer);
		WebElement fileList = (new WebDriverWait(driver, Duration.ofSeconds(10)))
				.until(ExpectedConditions.presenceOfElementLocated(By.className("fileList")));

		// Before file download, remove old instance of it if exists
		if (Files.exists(Paths.get(Config.getInstance().getAssembliesPathOnDisk() + "\\archive.zip"))) {
			try {
				Files.delete(Paths.get(Config.getInstance().getAssembliesPathOnDisk() + "\\archive.zip"));
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			}
		}

		driver.findElement(By.cssSelector("a[href$='archive.zip']")).click();

		// Wait for zipped archive download to finish
		int timeoutSeconds = 0;
		while (Files.exists(Paths.get(Config.getInstance().getAssembliesPathOnDisk() + "\\archive.zip.part"))) {
			if (timeoutSeconds >= 90) {
				System.out.println(" Timeout error after waiting for assembly download - (90sec)");
				break;
			}
			try {
				timeoutSeconds++;
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("InterruptedException: " + e.getMessage());
			}
		}
		this.driver.close();
		System.out.println("DOWNLOADED ARTIFACTS AT: " + Config.getInstance().getAssembliesPathOnDisk());
		if (!Files.exists(Paths.get(Config.getInstance().getAssembliesPathOnDisk() + "\\archive.zip"))) {
			System.out.println("Downloading artifacts has failed");
		} else {
			unzip(Config.getInstance().getAssembliesPathOnDisk(),
					Config.getInstance().getAssembliesPathOnDisk() + "\\archive.zip");
		}
	}

	/**
	 * unzip downloaded archive
	 * 
	 * @param targetDir   directory of unzipped archive
	 * @param zipFilename zip file to be unzipped
	 */
	private void unzip(String targetDir, String zipFilename) {
		Path targetDirPath = Paths.get(targetDir);
		try (ZipFile zipFile = new ZipFile(zipFilename)) {
			zipFile.stream().parallel() // enable multi-threading
					.forEach(e -> unzipEntry(zipFile, e, targetDirPath));
		} catch (IOException e) {
			throw new RuntimeException("Error opening zip file '" + zipFilename + "': " + e, e);
		}
	}

	/**
	 * Copies from the archive to unzipped folder directory
	 * 
	 * @param zipFile   zipped archive
	 * @param entry     path of a single file in the archive
	 * @param targetDir unzipping directory to copy entries to
	 */
	private void unzipEntry(ZipFile zipFile, ZipEntry entry, Path targetDir) {
		try {
			Path targetPath = targetDir.resolve(Paths.get(entry.getName()));
			if (Files.isDirectory(targetPath)) {
				Files.createDirectories(targetPath);
			} else {
				Files.createDirectories(targetPath.getParent());
				try (InputStream in = zipFile.getInputStream(entry)) {
					Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error processing zip entry '" + entry.getName() + "': " + e, e);
		}
	}
}
