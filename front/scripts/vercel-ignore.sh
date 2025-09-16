if [ -z "$VERCEL_GIT_PREVIOUS_SHA" ]; then
  echo "No previous SHA — proceed with build"
  exit 1
fi

git fetch --depth=1 origin "$VERCEL_GIT_COMMIT_REF" || true

CHANGED=$(git diff --name-only "$VERCEL_GIT_PREVIOUS_SHA" "$VERCEL_GIT_COMMIT_SHA" || true)
echo "Changed files:"
echo "$CHANGED"

echo "$CHANGED" | grep -q '^front/' && { echo "front/ changed -> running build"; exit 1; }

echo "no changes in front/ -> skipping build"
exit 0
