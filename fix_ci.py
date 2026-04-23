import yaml

with open(".github/workflows/autorelease.yml", "r") as f:
    data = f.read()

# Replace uses: dorny/test-reporter@... with a v3 or a different way?
# Wait, the error is: Node.js 20 actions are deprecated... dorny/test-reporter, jdx/mise-action
# To fix: update them.
# dorny/test-reporter@v3 (Wait, let's search if v3 exists)
# jdx/mise-action@v2 -> v2 uses node20. Let's see.
