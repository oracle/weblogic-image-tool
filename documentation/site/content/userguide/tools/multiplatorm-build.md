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
6. Completely disabled selinux seems to resolve issue,  check also /var/log/messages  , /var/log/audit/audit.log 

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

podman run --platform <emulated platform> results in
ERROR: /etc/mtab symlink operation not permitted.

podman info shows it is using native overlays,  changing it to individual fuse overlays works

sudo dnf install fuse-overlayfs

create $USER/.config/containers/storage.conf

[storage]
driver = "overlay"

[storage.options.overlay]
mount_program = "/usr/bin/fuse-overlayfs"


completely disabled selinux (rather than permissive by setenforce 0)

/etc/selinux/config


podman system reset

and install qemu again.


==============================

cat /var/log/messages  

Dec 31 20:23:46 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64. For complete SELinux messages run: sealert -l 3e9c70c9-80e1-4abc-8a12-39f2846b5a10
Dec 31 20:23:46 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64.#012#012*****  Plugin catchall (100. confidence) suggests   **************************#012#012If you believe that qemu-aarch64 should be allowed read access on the qemu-aarch64 file by default.#012Then you should report this as a bug.#012You can generate a local policy module to allow this access.#012Do#012allow this access for now by executing:#012# ausearch -c 'sh' --raw | audit2allow -M my-sh#012# semodule -X 300 -i my-sh.pp#012
Dec 31 20:23:46 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64. For complete SELinux messages run: sealert -l 3e9c70c9-80e1-4abc-8a12-39f2846b5a10
Dec 31 20:23:46 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64.#012#012*****  Plugin catchall (100. confidence) suggests   **************************#012#012If you believe that qemu-aarch64 should be allowed read access on the qemu-aarch64 file by default.#012Then you should report this as a bug.#012You can generate a local policy module to allow this access.#012Do#012allow this access for now by executing:#012# ausearch -c 'sh' --raw | audit2allow -M my-sh#012# semodule -X 300 -i my-sh.pp#012
Dec 31 20:23:49 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64. For complete SELinux messages run: sealert -l 3e9c70c9-80e1-4abc-8a12-39f2846b5a10
Dec 31 20:23:49 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64.#012#012*****  Plugin catchall (100. confidence) suggests   **************************#012#012If you believe that qemu-aarch64 should be allowed read access on the qemu-aarch64 file by default.#012Then you should report this as a bug.#012You can generate a local policy module to allow this access.#012Do#012allow this access for now by executing:#012# ausearch -c 'sh' --raw | audit2allow -M my-sh#012# semodule -X 300 -i my-sh.pp#012
Dec 31 20:23:49 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64. For complete SELinux messages run: sealert -l 3e9c70c9-80e1-4abc-8a12-39f2846b5a10
Dec 31 20:23:49 ol8-machine setroubleshoot[239987]: SELinux is preventing /usr/bin/qemu-aarch64 from read access on the file qemu-aarch64.#012#012*****  Plugin catchall (100. confidence) suggests   **************************#012#012If you believe that qemu-aarch64 should be allowed read access on the qemu-aarch64 file by default.#012Then you should report this as a bug.#012You can generate a local policy module to allow this access.#012Do#012allow this access for now by executing:#012# ausearch -c 'sh' --raw | audit2allow -M my-sh#012# semodule -X 300 -i my-sh.pp#012

========

When you encounter an error stating that Podman build is being prevented by SELinux from allowing "qemu-aarch64" to read access, it means that your system's Security Enhanced Linux (SELinux) policy is blocking the necessary permissions for the QEMU emulator to read files within the containerized environment, likely due to strict access controls; to fix this, you can create a custom SELinux policy rule to grant the required read access to QEMU within the container context. 
Key points to understand:
SELinux and Containers:
SELinux assigns security labels to files and processes, which can sometimes restrict access when running containers, especially when using features like volume mounts. 
QEMU and ARM emulation:
If you're building a container with an ARM architecture using "qemu-aarch64" on a different architecture, SELinux might need additional permissions to allow the emulation process to read necessary files. 
How to troubleshoot and fix:
Check SELinux logs:
Run sudo grep 'qemu-aarch64' /var/log/audit/audit.log to see if there are any SELinux AVC (Access Vector Cache) messages related to the QEMU process. 
Temporarily disable SELinux (not recommended):
To quickly test if SELinux is the issue, run sudo setenforce 0. 
Important: Re-enable SELinux with sudo setenforce 1 after testing. 
Create a custom SELinux policy:
Identify the issue: Analyze the SELinux logs to determine which specific file access is being denied and the SELinux context of the file. 
Use sepolicy:
Edit the SELinux policy file (/etc/selinux/policy/base/container_file_t) to add a rule allowing the "qemu-aarch64" process to read files with the relevant context. 
Example:
Code

            allow container_file_t qemu_aarch64_t:file r_file_perms;  
Compile and reload the policy:
Run make within the /etc/selinux/policy directory to compile the modified policy.
Restart the SELinux daemon to apply the changes (usually done with a system reboot). 
Important Considerations:
Security implications:
Always carefully review and test custom SELinux policy changes as granting too much access can introduce security vulnerabilities. 
Consult documentation:


============

Step 1: Adjust the Command
The command ausearch -c 'sh' --raw | audit2allow -M my-sh might be too broad. Try to focus on the specific denials for qemu-aarch64. Use:
bash
ausearch -m avc -c qemu-aarch64 --raw | audit2allow -M my-qemu
This command will look for AVC (Access Vector Cache) messages related specifically to qemu-aarch64.

Step 2: Review and Modify Policy
If the above command still results in errors, you might need to manually edit the generated .te file before compiling:
After running the audit2allow command, it should create my-qemu.te in your current directory. Open this file:
bash
nano my-qemu.te
Look for any lines starting with mlsconstrain. If they are causing issues, you might want to comment them out or remove them since they might not be necessary for your specific use case:
text
# mlsconstrain file { ioctl read lock execute execute_no_trans } ((h1 dom h2 -Fail-)  or (t1 != mcs_constrained_type -Fail-) );
# mlsconstrain file { write setattr append unlink link rename } ((h1 dom h2 -Fail-)  or (t1 != mcs_constrained_type -Fail-) );
After modifying, save the file.

Step 3: Compile and Load the Policy
Try to compile the policy again:
bash
checkmodule -M -m -o my-qemu.mod my-qemu.te
semodule_package -o my-qemu.pp -m my-qemu.mod
semodule -i my-qemu.pp


========

TODO:

podman does not take --load or --push
docker must have --load or --push for multiplatform build  

OL9 (not sure about OL8) defaults overlays doesn't work with emulator run 



 
