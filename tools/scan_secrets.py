# -*- coding: utf-8 -*-
"""
scan_secrets.py — 仓库敏感信息扫描器（安全审计工具）

扫描范围：
  1. git 已跟踪content：每个版本树一次 `git grep`（含二进制 -a），逐行定位
  2. 全部本地分支的完整历史（`git rev-list --all`）
  3. 已跟踪文件的filename（密钥/凭据类命名）
  4. 工作区未跟踪文件（提醒用：未跟踪文件不会被推送，但仍建议处理）

用法：
  python tools/scan_secrets.py            # 全量（HEAD + 全历史）
  python tools/scan_secrets.py --head     # 只扫 HEAD 工作树（快）

退出码：0 = 无命中；1 = 发现疑似敏感信息（输出 file:line 定位）。
注意：命中是"疑似"，需人工复核（万智牌卡名 / 库代码字符串 / 官方占位符可能误报）。
"""
import argparse
import os
import re
import subprocess
import sys

# ---- filename模式（真正敏感的命名）----
NAME_PAT = re.compile(
    r"(?i)(^|/)(\.env($|\.)|id_rsa|id_ed25519|\.pem$|\.key$|\.p12$|\.pfx$|\.jks$|\.keystore$|"
    r"\.git-credentials|\.netrc$|\.npmrc$|credentials?($|\.)|\.htpasswd|"
    r"forge\.profile\.properties$|forge\.preferences$|.*\.prefs$|.*\.log$|agenta?\.md|"
    r"session[-_]?log|deepcode|dsh[-_]?(ssh|config)|wallet|passwords?($|\.)|"
    r"\.kube/|\.aws/|\.azure/|docker/config\.json|\.sentryclirc|\.pypirc|"
    r"local\.settings\.xml$|settings\.xml$|sentry\.properties$|"
    r"application.*\.(yml|yaml|properties|json)$|secret.*\.(json|ya?ml|ini|cfg)$)"
)

# ---- content模式（密钥/令牌格式），每条对应一个 git grep -e 参数 ----
CONTENT_PATTERNS = [
    r"-----BEGIN [A-Z ]*PRIVATE KEY-----",
    r"-----BEGIN PGP",
    r"gh[pous]_[A-Za-z0-9]{20,}",
    r"github_pat_[A-Za-z0-9_]{20,}",
    r"glpat-[A-Za-z0-9_-]{20,}",
    r"AKIA[0-9A-Z]{16}",
    r"AIza[0-9A-Za-z_-]{30,}",
    r"xox[baprs]-[A-Za-z0-9-]{10,}",
    r"sk-[A-Za-z0-9]{32,}",
    r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}",
    r"(?i)(api[_-]?key|client[_-]secret|access[_-]key|secret[_-]key|auth[_-]?token)\s*[=:]\s*['\"]?[A-Za-z0-9+/_.-]{16,}",
    r"(?i)(password|passwd)\s*[=:]\s*['\"][^'\"]{6,}['\"]",
]


def run(args, binary=False):
    p = subprocess.run(args, capture_output=True)
    return p.stdout if binary else p.stdout.decode("utf-8", "replace")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--head", action="store_true", help="只扫描 HEAD 工作树")
    args = ap.parse_args()

    hits = []
    revs = ["HEAD"] if args.head else run(["git", "rev-list", "--all"]).split()
    print("scanning tree revisions: %d" % len(revs))

    for rev in revs:
        # 1) filename检查
        for f in run(["git", "ls-tree", "-r", "--name-only", rev]).splitlines():
            if NAME_PAT.search(f):
                hits.append("%s | filename | %s" % (rev[:12], f))
        # 2) content检查：一次 git grep（-a 含二进制；-n 行号）
        grep_args = ["git", "grep", "-a", "-n", "-E"]
        for pat in CONTENT_PATTERNS:
            grep_args += ["-e", pat]
        grep_args += [rev, "--"]
        out = run(grep_args)
        if out.strip():
            for line in out.splitlines():
                hits.append("%s | content | %s" % (rev[:12], line.strip()))

    # 3) 工作区未跟踪文件提醒（仅提醒，不判失败）；排除扫描器自身
    self_path = os.path.abspath(__file__).replace("\\", "/")
    for f in run(["git", "ls-files", "--others", "--exclude-standard"]).splitlines():
        if os.path.abspath(f).replace("\\", "/") == self_path:
            continue
        if NAME_PAT.search(f):
            hits.append("UNTRACKED | filename | %s (untracked, never pushed)" % f)
        try:
            with open(f, "rb") as fh:
                data = fh.read()
            for pat in CONTENT_PATTERNS:
                m = re.search(pat.encode(), data)
                if m:
                    hits.append("UNTRACKED | content | %s" % f)
                    break
        except OSError:
            pass

    if hits:
        print("\n=== SUSPECTED SENSITIVE INFO: %d (manual review required)===" % len(hits))
        for h in hits:
            print(" ", h)
        sys.exit(1)
    print("\nNO HITS - repo is clean")
    sys.exit(0)


if __name__ == "__main__":
    main()

