import os
import xml.etree.ElementTree as ET
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
import json

@dataclass
class ComparisonConfig:
    old_file: str
    new_file: str
    tags_to_compare: List[str]


class XMLComparator:
    def __init__(self, old_folder: str, new_folder: str):
        self.old_folder = old_folder
        self.new_folder = new_folder
        
    def element_to_string(self, element: ET.Element, indent: int = 0) -> str:
        result = []
        indentation = "\t" * indent
        
        # Create opening tag with attributes
        attrs = ""
        for key, value in element.attrib.items():
            attrs += f' {key}="{value}"'
        
        # Add opening tag
        result.append(f"{indentation}<{element.tag}{attrs}>")
        
        # Process text content if present
        if element.text and element.text.strip():
            result.append(f"{indentation}\t{element.text.strip()}")
        
        # Process children recursively
        for child in element:
            result.append(self.element_to_string(child, indent + 1))
        
        # Add closing tag
        result.append(f"{indentation}</{element.tag}>")
        
        return "\n".join(result)
    
    def get_element_signature(self, element: ET.Element) -> str:
        attrs = "".join(f"{k}={v}" for k, v in sorted(element.attrib.items()))
        text = element.text.strip() if element.text else ""
        return f"{element.tag}|{attrs}|{text}"
    
    def find_tag_in_tree(self, root: ET.Element, target_tag: str) -> Optional[Tuple[ET.Element, List[str]]]:
        def _find_with_path(element, tag, current_path=[]):
            if element.tag == tag:
                return element, current_path
                
            for child in element:
                result = _find_with_path(child, tag, current_path + [element.tag])
                if result[0] is not None:
                    return result
                    
            return None, []
        
        return _find_with_path(root, target_tag)
    
    def compare_xml_tags(self, old_file: str, new_file: str, target_tag: str) -> Tuple[Optional[List[ET.Element]], Optional[List[ET.Element]]]:
        try:
            old_tree = ET.parse(old_file)
            new_tree = ET.parse(new_file)
        except Exception as e:
            print(f"Error parsing XML files: {e}")
            return None, None
        
        old_root = old_tree.getroot()
        new_root = new_tree.getroot()
        
        old_result = self.find_tag_in_tree(old_root, target_tag)
        new_result = self.find_tag_in_tree(new_root, target_tag)
        
        # Unpack the results
        if old_result[0] is None or new_result[0] is None:
            print(f"Tag '{target_tag}' not found in one or both files.")
            return None, None
            
        old_target, old_path = old_result
        new_target, new_path = new_result
        
        # Check if tags are at the same hierarchy level
        if old_path != new_path:
            print(f"Tag '{target_tag}' is not at the same hierarchy level in both files.")
            return None, None
        
        old_signatures = {}
        for child in old_target:
            signature = self.get_element_signature(child)
            old_signatures[signature] = child
        
        new_signatures = {}
        for child in new_target:
            signature = self.get_element_signature(child)
            new_signatures[signature] = child
        
        # Find additions (in new file but not in old file)
        additions = [
            child for signature, child in new_signatures.items() 
            if signature not in old_signatures
        ]
        
        # Find removals (in old file but not in new file)
        removals = [
            child for signature, child in old_signatures.items() 
            if signature not in new_signatures
        ]
        
        return additions, removals


class XMLComparisonManager:
    
    def __init__(self, base_dir: str = '.'):
        self.base_dir = base_dir
        self.old_dir = os.path.join(base_dir, 'old')
        self.new_dir = os.path.join(base_dir, 'new')
        self.comparator = XMLComparator(self.old_dir, self.new_dir)
        self.configs: List[ComparisonConfig] = []
        
    def add_comparison(self, old_file: str, new_file: str, tags: List[str]):
        config = ComparisonConfig(
            old_file=old_file,
            new_file=new_file,
            tags_to_compare=tags
        )
        self.configs.append(config)
        
    def load_config_from_file(self, config_file: str):
        try:
            with open(config_file, 'r') as f:
                config_data = json.load(f)
            
            for item in config_data:
                self.add_comparison(
                    item['old_file'],
                    item['new_file'],
                    item['tags']
                )
        except Exception as e:
            print(f"Error loading configuration: {e}")
            
    def save_config_to_file(self, config_file: str):
        config_data = []
        for config in self.configs:
            config_data.append({
                'old_file': config.old_file,
                'new_file': config.new_file,
                'tags': config.tags_to_compare
            })
            
        with open(config_file, 'w') as f:
            json.dump(config_data, f, indent=4)
            
    def run_all_comparisons(self) -> Dict[str, Any]:
        results = {}
        
        for config in self.configs:
            old_file_path = os.path.join(self.old_dir, config.old_file)
            new_file_path = os.path.join(self.new_dir, config.new_file)
            
            if not os.path.exists(old_file_path):
                print(f"Error: Old file does not exist: {old_file_path}")
                continue
                
            if not os.path.exists(new_file_path):
                print(f"Error: New file does not exist: {new_file_path}")
                continue
                
            file_results = {}
            for tag in config.tags_to_compare:
                additions, removals = self.comparator.compare_xml_tags(
                    old_file_path, new_file_path, tag
                )
                
                # Mark tags with hierarchy mismatches or not found tags as "Invalid Input"
                if additions is None or removals is None:
                    file_results[tag] = {
                        'invalid': True
                    }
                else:
                    file_results[tag] = {
                        'invalid': False,
                        'additions': [self.comparator.element_to_string(elem) for elem in additions],
                        'removals': [self.comparator.element_to_string(elem) for elem in removals]
                    }
            
            results[f"{config.old_file} -> {config.new_file}"] = file_results
            
        return results
    
    def print_results(self, results: Dict[str, Any]):
        for comparison, file_results in results.items():
            print(f"\n=== Comparison: {comparison} ===")
            
            for tag, changes in file_results.items():
                print(f"\nTag: <{tag}>")
                
                # Check if this tag has invalid comparison
                if changes.get('invalid', False):
                    print("Invalid Input")
                    continue
                
                print("Additions:")
                if changes['additions']:
                    for addition in changes['additions']:
                        print(addition)
                else:
                    print("\tNone")
                    
                print("\nRemovals:")
                if changes['removals']:
                    for removal in changes['removals']:
                        print(removal)
                else:
                    print("\tNone")


def ensure_directories():
    os.makedirs('old', exist_ok=True)
    os.makedirs('new', exist_ok=True)


def main():
    ensure_directories()
    
    manager = XMLComparisonManager()
    
    config_file = 'comparison_config.json'
    if os.path.exists(config_file):
        print(f"Loading configuration from {config_file}...")
        manager.load_config_from_file(config_file)
    else:
        print("No configuration file found. Using default comparisons...")
        manager.add_comparison('file1.xml', 'file2.xml', ['a', 'root'])
    
    print("\nRunning XML comparisons...")
    results = manager.run_all_comparisons()
    manager.print_results(results)


if __name__ == "__main__":
    main()
