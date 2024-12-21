Set up qemu (unless the machine is already set up for that)

docker:
 docker run --privileged --rm tonistiigi/binfmt --install all
 
podman:
 sudo podman run --privileged --rm tonistiigi/binfmt --install all
 
notes:

  In podman environment, sometimes the image build process stuck during installation of fmw, this can be :
  
1. space issue, default is in /var/lib/containers/storage - check utilization by df -h .   Change graphroot in /etc/containers/storage.conf for system wide or change the personal settings, see below.
2. try turn off selinux  sudo setenforce 0 , it may come out with more information.
3. try ps axwww | grep qemu   to see if there are multiple java processes for the installation.
4. after podman system prune - I have to run the qemu setup again 
5. qemu is not installed properly,  the above method put files in /proc/sys/fs/binfmt_misc/qemu*


---

echo 'username:100000:65536' | sudo tee -a /etc/subuid
echo 'username:100000:65536' | sudo tee -a /etc/subgid

echo 'user.max_user_namespaces=28633' | sudo tee -a /etc/sysctl.d/userns.conf
sudo sysctl -p /etc/sysctl.d/userns.conf

mkdir -p /path/to/storage/$USER

mkdir -p ~/.config/containers
echo "[storage]
driver = \"overlay\"
rootless_storage_path = \"/path/to/storage/$USER\"" > ~/.config/containers/storage.conf


sudo loginctl enable-linger username

========

TODO:

podman does not take --load or --push
docker must have --load or --push for multiplatform build  

 
