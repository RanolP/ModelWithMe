# ModelWithMe
Create a 3D model in minecraft via building.

## 목표

플러그인으로 Armourer's Workshop(이하 AW)의 재현

### 추가하는 블록

#### Armourer

모델링을 위한 영역을 선포하고 모델의 세부 사항을 조정하는 블록

사용 가능한 동작들

- Clear (빌딩 가이드 내부 빌딩 블록을 전부 제거)
- Save (임의 이름으로 저장)
- Load (임의 이름의 모델을 로드)
- Change Part (Head, Chest, Legs, Feet, Sword, Bow, Arrow, Block 중 하나로 파트를 변경)

#### (Glass) Equipment Cube

모델 그 자체를 나타내는 블록

### 추가하는 도구

#### Glower

(Glass) Equipment Cube의 Glowing 상태를 끄거나 켬

#### Paint Tool

(Glass) Equipment Cube의 색상을 설정함

#### Dodge/Burn Tool

좌클릭으로 더 밝게, 우클릭으로 더 어둡게 색을 조정함

#### Color Picker

색상을 선택해 팔레트에 추가함

#### Mannequin

마네킹을 소환해 모델을 입혀볼 수 있게 함

### 명령어

#### /mwm toolbox

추가한 블록 및 아이템을 꺼낼 수 있는 도구 상자

### 구현 세부사항

- (Glass) Equipment Cube
  - MVP: 테라코타와 색유리로 표현 
  - Goal: 플레이어 머리를 쓴 갑옷 거치대 생성기를 통해 커스텀 단색 블록
  - Glowing의 표현: 발광이 걸린 마그마 큐브를 블록에 겹치도록 함
  - Idea: 텍스트 텍스쳐와 표지판을 활용한 단면 텍스쳐링
    - 한계: 블록의 설치/파괴를 방해함. 빠른 리소스팩 미리보기에는 활용할 수 있을듯
- Armourer's Hologram
  - MVP: 그-없
  - Goal: 머리/가슴/다리/발/검/활/화살의 홀로그램을 1) 블록 패킷 2) 파티클 패킷 으로 보여줌
    - 블록 패킷
      - Pros: 홀로그램 근처에 블록을 두는 경우가 많은 만큼 공중에 블록을 두기 용이해짐
      - Cons: 검의 손잡이와 같이 홀로그램과 겹쳐야 하는 경우 난감함
    - 파티클 패킷
      - Pros: 외곽선만 간결히 나타내므로 건축을 방해하지 않음
      - Cons: 공중에 블록을 둘 때 참고 이상의 역할을 못 함
    - 따라서, 두 패킷 모두를 활용해 서로 전환가능하게 하도록 함
- Preview Model
  - 모델을 프리뷰하기 위해선 아주 귀찮게도 리소스 팩을 새로 받아야 한다
  - 하지만 더 작은 크기로 보며 애자일하게 수정하기를 원하지 모델 자체를 원하지 않으므로 작은 갑옷 거치대 여럿을 묶어 커스텀 단색 블록으로 표현하는 것이 좋을 듯
  - 특히, 블록 프리뷰의 경우 텍스트 텍스쳐와 표지판을 활용할 방안을 모색해보는 것도 좋겠음
- Mannequin
  - 마네킹은 모델을 보여주는 데에 아주 탁월한 도구임
  - 크게 신경쓸 건 없고, 저장된 모델 불러오기랑 각도 조절, 작게 만들기 정도만 해주면 될듯.
