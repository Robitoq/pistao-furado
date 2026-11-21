import re
SRC = '/home/claude/proj/pistao-furado-main/app/src/main/java/com/ferrovelho/game/CarArt.kt'

def parse_shapes():
    import json, os
    j = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'shapes_data.json')
    if os.path.exists(j):
        d = json.load(open(j))
        for v in d.values(): v['head'] = tuple(v['head'])
        return d
    txt = open(SRC, encoding='utf-8').read()
    i = txt.index('val CAR_SHAPES')
    txt = txt[i:]
    out = {}
    # cada entrada: Body.X to CarShape( f(...), listOf(f(..), f(..)), f(doors), rear, front, r, a to b, ...)
    for m in re.finditer(r'Body\.(\w+) to CarShape\(', txt):
        name = m.group(1)
        j = m.end()
        depth = 1; k = j
        while depth:
            c = txt[k]
            if c == '(': depth += 1
            elif c == ')': depth -= 1
            k += 1
        body = txt[j:k - 1]
        body = re.sub(r'//[^\n]*', '', body)
        fs = re.findall(r'(?<![A-Za-z])f\(([^)]*)\)', body)
        arrs = [[float(v.replace('f', '')) for v in a.split(',') if v.strip()] for a in fs]
        rest = re.sub(r'listOf\(', '', body)
        rest = re.sub(r'(?<![A-Za-z])f\([^)]*\)', 'F', rest)
        # restante: rear, front, r, head, flags
        tail = rest.split('F')[-1]
        nums = re.findall(r'-?\d+\.?\d*f', tail)
        nums = [float(n.replace('f', '')) for n in nums]
        n_glass = len(arrs) - 2  # body, glass..., doors
        out[name] = dict(body=arrs[0], glass=arrs[1:1 + n_glass], doors=arrs[-1],
                         rear=nums[0], front=nums[1], r=nums[2], head=(nums[3], nums[4]),
                         bed='bed = true' in tail, chrome='chrome = true' in tail)
    return out

if __name__ == '__main__':
    s = parse_shapes()
    for k, v in s.items():
        print(k, len(v['body']) // 2, 'pts', len(v['glass']), 'glass', v['doors'], v['rear'], v['front'], v['r'], v['head'], v['bed'], v['chrome'])
