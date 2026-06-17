# OpenSpec workflow (MANDATORY)                                                                                        
                                                                                                                    
This project uses [OpenSpec](openspec/) for spec-driven development. **Before writing                                  
any feature or non-trivial code change, you MUST go through the OpenSpec flow.**                                       
Do not jump straight to editing source.                                                                                
                                                                                                                    
## Process                                                                                                             
                                                                                                                    
1. **Propose** — create a change under `openspec/changes/<change-id>/` with                                            
`proposal.md`, `design.md`, `tasks.md`, and a `specs/<capability>/spec.md`                                          
delta. Use the `openspec-propose` skill (or `openspec` CLI) to scaffold it.                                         
2. **Get sign-off** — the proposal/design must be reviewed before implementation.                                      
3. **Implement** — work through `tasks.md`, ticking items off. Use the                                                 
`openspec-apply-change` skill.                                                                                      
4. **Sync specs** — once the change ships, sync the delta into                                                         
`openspec/specs/` via `openspec-sync-specs`, then archive the change.                                               
                                                                                                                    
## Rules                                                                                                               
                                                                                                                    
- Spec deltas are written in **user-facing product behavior language**                                                 
(`### Requirement:` + `#### Scenario:` blocks), not implementation detail.                                           
- The filesystem is the source of truth; Room is an index cache that can be                                            
rebuilt from disk. Schema changes need a Room migration strategy in the tasks.                                       
- UI strings live in `res/values/strings.xml` (zh-CN) and                                                              
`res/values-en/strings.xml` (en). Code/comments stay in English.                                                     
- After implementing, verify with Detekt/ktlint (`./gradlew detekt ktlintCheck`).                                      
                                                                                                                    
## Trivial changes that DO NOT need a spec                                                                             
                                                                                                                    
- Pure typo / copy fix, log message, formatting                                                                        
- A single-line bug fix with no behavior contract change                                                               
- Updating an existing memory or doc that isn't a product spec                                                         
                                                                                                                    
When in doubt, propose first.

## Roadmap

[ROADMAP.md](ROADMAP.md) lists planned-but-unbuilt features (input, organization,
recall, safety-net, security). It is the upper index above OpenSpec: it decides
*what* to build next; each entry still goes through the OpenSpec flow above when
started. Check it before proposing a new feature to avoid duplicating or
contradicting a planned item.
