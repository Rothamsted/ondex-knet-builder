# 
# Run this the first time you clone ondex-full, if your git client doesn't download submodules automatically (i.e., 
# you see empty ondex-xxx folders)
# 
cd "$(dirname $0)"
printf "\n\n\n\tSetting up submodules\n\n"
git submodule update --init --recursive

printf "\n\n\n\tSwitching to master branch in each module\n\n"
for repo in ondex-base ondex-desktop ondex-integration-tests ondex-opt ondex-base	ondex-doc	ondex-knet-builder
do
  echo -e "\n\n$repo"
  cd "$repo"
  git checkout master
  cd ..
done
