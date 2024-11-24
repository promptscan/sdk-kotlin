# PromptScan SDK Java Example

This is a simple example project demonstrating how to use the PromptScan SDK in a Java application.

## Prerequisites

- Java 11 or later
- Maven
- GitHub account with access to the PromptScan SDK repository

## Setup

1. Configure GitHub Packages authentication by creating or editing `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

Replace `YOUR_GITHUB_USERNAME` with your GitHub username and `YOUR_GITHUB_TOKEN` with a GitHub Personal Access Token that has the `read:packages` scope.

2. Update the API key in `PromptScanExample.java` with your actual PromptScan API key.

## Running the Example

To run the example, execute the following command from the `examples/java` directory:

```bash
mvn compile exec:java -Dexec.mainClass="ai.promptscan.example.PromptScanExample"
```

## Code Explanation

The example demonstrates:
- Initializing the PromptScan SDK with your API key
- Scanning a prompt
- Handling and displaying the scan results

Check the `PromptScanExample.java` file for detailed code comments.
