""" Compile jni C++ bindings and add them as resources
to the project

"""

import argparse
import os
import platform
import sys

from qisys import ui
import qisys.sh
import qibuild.find
import qibuild.parsers

def copy_jni_resources(build_worktree):
    """ Create symlinks (or copy), the jni library and its
    dependencies from the build dirs to the resource folder

    """
    android = "android" in (build_worktree.build_config.active_build_config.name or "")
    # FIXME: boost when using a toolchain on linux ?
    libs = list()
    proj_to_lib = [
            ("libqi",          "qi"),
            ("qimessaging-jni", "qimessagingjni")]
    if android:
        proj_to_lib.append(
            ("qimessaging-jni", "gnustl_shared"))

    for project_name, library_name in proj_to_lib:
        try:
            project = build_worktree.get_build_project(project_name, raises=True)
            library_path = qibuild.find.find_lib([project.sdk_directory], library_name)
            libs.append(library_path)
        except qibuild.find.NotFound as e:
            ui.error(e)
            ui.error("Make sure qimessaging-jni has been built")
            sys.exit(1)
    toolchain = build_worktree.toolchain
    boost = toolchain.get_package("boost")
    if platform.system() == "Windows":
        boost_lib = os.path.join(boost.path, "bin")
    else:
        boost_lib = os.path.join(boost.path, "lib")

    boost_lib_files = [os.path.join(boost_lib, f) for f in os.listdir(boost_lib)
        if os.path.isfile(os.path.join(boost_lib, f))]
    libs_to_keep = ("chrono", "system", "filesystem", "locale", "thread",
    "regex", "program_options")
    boost_lib_files = [f for f in boost_lib_files if any(lib in f for lib in libs_to_keep)]
    boost_lib_files = [f for f in boost_lib_files if f.endswith(".so") or f.endswith(".dylib") or f.endswith(".dll")]
    libs.extend(boost_lib_files)

    java_proj = build_worktree.worktree.get_project("sdk/libqi-java/qimessaging")
    if android:
        dest = os.path.join(java_proj.path, "native-android")
    else:
        dest = os.path.join(java_proj.path, "native")
    qisys.sh.mkdir(dest)
    for lib_src in libs:
        lib_dest = os.path.join(dest, os.path.basename(lib_src))
        copy_or_link(lib_src, lib_dest)


def copy_or_link(src, dest):
    """ Create a symlink from src to dest, or copy the file
    if the operating system does not support it

    """
    if os.name == 'nt':
        qisys.sh.install(src, dest)
    else:
        qisys.sh.rm(dest)
        os.symlink(src, dest)

def main():
    parser = argparse.ArgumentParser()
    qibuild.parsers.cmake_build_parser(parser)
    args = parser.parse_args()
    build_worktree = qibuild.parsers.get_build_worktree(args)
    ui.info("Copying jni libraries as resources ...")
    copy_jni_resources(build_worktree)
    ui.info("Done")


if __name__ == "__main__":
    main()
