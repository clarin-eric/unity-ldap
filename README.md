Original location:
```
ip-145-102-25-242:unity-public wilelb$ git remote -v
origin    git://git.assembla.com/unity-public.git (fetch)
origin    git://git.assembla.com/unity-public.git (push)
```

Add assemle remote:
```
git remote add assembla git://git.assembla.com/unity-public.git
```

Update with upstream changes from assembla:
```
git pull assembla
```
