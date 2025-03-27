Requirements:

Black Duck Server licensed for Generative AI Snippet Scanning

**SARIFF Support works through Code scanning in GitHub and is available for all public repositories on GitHub . Code scanning is also available for private repositories owned by organizations that use GitHub Enterprise Cloud and have a license for GitHub Advanced Security.

Repo Settings → Code Security and Analysis → Enable CodeQL (you dont have to run the CodeQL Scans)
Instructions on usage:
Required Secrets and Variables - Never Hard code Secrets and Variables

To Add Secrets and Variable → Repo Settings to Secrets and Variables Drop Down to Actions

Required Repo Variables:

HOSTNAME: <Your Black Duck Server URL>

Required Repo Secrets:

BLACK_DUCK_API_TOKEN: <Black Duck API Token> (**Needs read and write permissions**)

WORKFLOW_GIT_ACCESS_TOKEN: <GitHub Token>
Add the following two actions to your repo in your .github/workflows directory (create this if it does not exist)

After adding the two actions to your repo, edit your files and create a pull request. The snippet scan action will kick off and add comments to the PR Comment section with any found snippets.

After merging your PR, the second action will run to upload the SARIF file containing your snippet data accessible in the repo’s Security Tab under Code Scanning. See above for SARIF support requirements.

Action #1
    
        name: Snippet Analysis
        
        on:
          pull_request:
            types: [opened, reopened]
        
        jobs:
          analyze-snippets: 
            runs-on: ubuntu-latest
            permissions:
              issues: write
              pull-requests: write
            steps:
      - name: Fetch Bearer Token 
        run: |
          RESPONSE=$(curl -k -X POST -H "Authorization: token ${{ secrets.BLACK_DUCK_API_TOKEN }}" "https://${{ vars.HOSTNAME }}/api/tokens/authenticate")
          BEARER_TOKEN=$(echo "${RESPONSE}" | jq -r '.bearerToken')
          echo "BEARER_TOKEN=${BEARER_TOKEN}" >> $GITHUB_ENV  
      
      - name: Checkout code
        uses: actions/checkout@v3  
        with:
          ref: ${{ github.event.pull_request.head.ref }}
          token: ${{ secrets.WORKFLOW_GIT_ACCESS_TOKEN }}
      
      - name: Get list of changed files 
        run: |
          URL="https://api.github.com/repos/${GITHUB_REPOSITORY}/pulls/${{ github.event.pull_request.number }}/files"
          echo "Requesting URL: $URL"
          RESPONSE=$(curl -s -X GET -G -H "Authorization: token ${{ secrets.WORKFLOW_GIT_ACCESS_TOKEN }}" $URL)
          echo "API Response: $RESPONSE"
          FILES=$(echo "$RESPONSE" | jq -r '.[] | .filename' | tr '\n' ' ')
          echo "FILES=${FILES}" >> $GITHUB_ENV
        
      - name: Query for snippet matches
        run: |
          rm -rf .snippet-licenses
          mkdir .snippet-licenses
          FILEPATHS="${{ env.FILES }}"
          
          count=1                          
          for filepath in $FILEPATHS; do       
            filename=$(echo "$filepath" | tr '/' '_')-snippet-analysis.json
            echo "File path: $filepath, File name: $filename"
            RES=$(curl -k --location --request POST "https://${{ vars.HOSTNAME }}/api/snippet-matching" --header "Authorization: Bearer ${BEARER_TOKEN}" --header "Content-Type: text/plain" --data-binary "@$filepath")
            echo "$RES" | jq '.' > ".snippet-licenses/$filename"
            if [ $count -eq 10 ]
            then
              break
            fi
            ((count++))
          done
          
      - name: Generate SARIF Report
        run: |
          # Initialize the SARIF template
          echo '{
            "$schema": "http://json.schemastore.org/sarif-2.1.0",
            "version": "2.1.0",
            "runs": [
              {
                "tool": {
                  "driver": {
                    "name": "Custom License Analyzer",
                    "version": "1.0",
                    "informationUri": "https://example.com"
                  }
                },
                "results": []
              }
            ]
          }' > sarif_template.json
      
          # Process each analysis result and append to the SARIF results
          for file in .snippet-licenses/*; do
            jq --arg file "$file" --slurpfile resultsArray "$file" '
              .runs[0].results += [
                $resultsArray[].snippetMatches[] | .[] | {
                  ruleId: (.licenseDefinition.spdxId // "unknown"),
                  level: "warning",
                  message: {
                    text: ("License match found: " + .licenseDefinition.licenseDisplayName)
                  },
                  locations: [
                    {
                      physicalLocation: {
                        artifactLocation: {
                          uri: $file,
                          uriBaseId: "%SRCROOT%"
                        },
                        region: {
                          startLine: .regions.sourceStartLines[0],
                          endLine: .regions.sourceEndLines[0]
                        }
                      }
                    }
                  ]
                }
              ]
            ' sarif_template.json > temp.json && mv temp.json sarif_template.json
          done
      
          # Rename the final SARIF file
          mv sarif_template.json snippet-analysis.sarif
          
      - name: Generate PR comment with findings
        id: comment
        uses: actions/github-script@v6
        with:
          github-token: ${{secrets.WORKFLOW_GIT_ACCESS_TOKEN}}
          script: |
            const fs = require('fs');
            const folderPath = '.snippet-licenses';
            
            let commentBody = '## Snippet License Analysis Results\n\n';
            fs.readdirSync(folderPath).forEach(file => {
              const analysisResult = JSON.parse(fs.readFileSync(`${folderPath}/${file}`, 'utf8'));
              const filePath = file.replace(/_/g, '/').replace('-snippet-analysis.json', '');
      
              commentBody += `### File: [${filePath}](https://github.com/${context.repo.owner}/${context.repo.repo}/blob/${context.sha}/${filePath})\n`;
             
              if (analysisResult.snippetMatches && Object.keys(analysisResult.snippetMatches).length > 0) {
                Object.keys(analysisResult.snippetMatches).forEach(licenseType => {
                  analysisResult.snippetMatches[licenseType].forEach(match => {
                    const projectName = match.projectName ? match.projectName : "N/A";
                    const releaseVersion = match.releaseVersion ? match.releaseVersion : "N/A";
                    const licenseName = match.licenseDefinition && match.licenseDefinition.licenseDisplayName ? match.licenseDefinition.licenseDisplayName : "Unknown License";
                    const lines = `L${match.regions.sourceStartLines[0]}-L${match.regions.sourceEndLines[0]}`;
                    const fileUrl = `https://github.com/${context.repo.owner}/${context.repo.repo}/blob/${context.sha}/${filePath}#${lines}`;
                    commentBody += ` - Project: ${projectName}, Version: ${releaseVersion}\n`;
                    commentBody += ` - Path: [${filePath}#${lines}](${fileUrl})\n`;
                    commentBody += `   - License Name: ${licenseName}\n`;
                    commentBody += `   - License Type: ${licenseType}\n`;
                    
                  });
                });
              } else {
                commentBody += 'No significant matches found.\n';
              }
              commentBody += '\n';
            });
    
            const issue_number = context.issue.number;
            const owner = context.repo.owner;
            const repo = context.repo.repo;
            
            const comment = await github.rest.issues.createComment({
              owner,
              repo,
              issue_number,
              body: commentBody
            });


      
      - name: Commit and push snippet analysis results
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action"
          git add .
          git diff --staged --quiet || git commit -m "Adding license findings via GitHub Actions"
          git push || echo "No changes to commit"

Action #2

        name: "Upload SARIF"

        on:
          push:
            branches:
              - main
        
        jobs:
          build:
            runs-on: ubuntu-latest
            permissions:
              # required for all workflows
              security-events: write
              # only required for workflows in private repositories
              actions: read
              contents: read
            steps:
              # This step checks out a copy of your repository.
              - name: Checkout repository
                uses: actions/checkout@v4
              - name: Upload SARIF file
                uses: github/codeql-action/upload-sarif@v3
                with:
                  # Path to SARIF file relative to the root of the repository
                  sarif_file: snippet-analysis.sarif
                  # Optional category for the results
                  # Used to differentiate multiple results for one commit
                  category: Snippet Analysis
        

