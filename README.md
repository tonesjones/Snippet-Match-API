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

